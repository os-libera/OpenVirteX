/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.messages;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.FlowTable;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.UnknownActionException;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.OVXUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

public class OVXFlowMod extends OFFlowMod implements Devirtualizable {


    private final Logger log = LogManager.getLogger(OVXFlowMod.class.getName());

    private OVXSwitch sw = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();

    private long ovxCookie = -1;

    @Override
    public void devirtualize(final OVXSwitch sw) {
        /* Drop LLDP-matching messages sent by some applications */
        if (this.match.getDataLayerType() == Ethernet.TYPE_LLDP) {
            return;
        }

        this.sw = sw;
        FlowTable ft = this.sw.getFlowTable();

        int bufferId = OVXPacketOut.BUFFER_ID_NONE;
        if (sw.getFromBufferMap(this.bufferId) != null) {
            bufferId = sw.getFromBufferMap(this.bufferId).getBufferId();
        }
        final short inport = this.getMatch().getInputPort();

        /* let flow table process FlowMod, generate cookie as needed */
        boolean pflag = ft.handleFlowMods(this.clone());

        /* used by OFAction virtualization */
        OVXMatch ovxMatch = new OVXMatch(this.match);
        ovxCookie = ((OVXFlowTable) ft).getCookie(this, false);
        ovxMatch.setCookie(ovxCookie);
        this.setCookie(ovxMatch.getCookie());

        for (final OFAction act : this.getActions()) {
            try {
                ((VirtualizableAction) act).virtualize(sw,
                        this.approvedActions, ovxMatch);
            } catch (final ActionVirtualizationDenied e) {
                this.log.warn("Action {} could not be virtualized; error: {}",
                        act, e.getMessage());
                ft.deleteFlowMod(ovxCookie);
                sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
                return;
            } catch (final DroppedMessageException e) {
                this.log.warn("Dropping flowmod {}", this);
                ft.deleteFlowMod(ovxCookie);
                // TODO perhaps send error message to controller
                return;
            }
        }

        final OVXPort ovxInPort = sw.getPort(inport);
        this.setBufferId(bufferId);

        if (ovxInPort == null) {
            if (this.match.getWildcardObj().isWildcarded(Flag.IN_PORT)) {
                /* expand match to all ports */
                for (OVXPort iport : sw.getPorts().values()) {
                    int wcard = this.match.getWildcards()
                            & (~OFMatch.OFPFW_IN_PORT);
                    this.match.setWildcards(wcard);
                    prepAndSendSouth(iport, pflag);
                }
            } else {
                this.log.error(
                        "Unknown virtual port id {}; dropping flowmod {}",
                        inport, this);
                sw.sendMsg(OVXMessageUtil.makeErrorMsg(
                        OFFlowModFailedCode.OFPFMFC_EPERM, this), sw);
                return;
            }
        } else {
            prepAndSendSouth(ovxInPort, pflag);
        }
    }

    private void prepAndSendSouth(OVXPort inPort, boolean pflag) {
        if (!inPort.isActive()) {
            log.warn("Virtual network {}: port {} on switch {} is down.",
                    sw.getTenantId(), inPort.getPortNumber(),
                    sw.getSwitchName());
            return;
        }
        this.getMatch().setInputPort(inPort.getPhysicalPortNumber());
        OVXMessageUtil.translateXid(this, inPort);
        try {
            if (inPort.isEdge()) {
                this.prependRewriteActions();
            } else {
                IPMapper.rewriteMatch(sw.getTenantId(), this.match);
                // TODO: Verify why we have two send points... and if this is
                // the right place for the match rewriting
                if (inPort != null
                        && inPort.isLink()
                        && (!this.match.getWildcardObj().isWildcarded(
                                Flag.DL_DST) || !this.match.getWildcardObj()
                                .isWildcarded(Flag.DL_SRC))) {
                    // rewrite the OFMatch with the values of the link
                    OVXPort dstPort = sw.getMap()
                            .getVirtualNetwork(sw.getTenantId())
                            .getNeighborPort(inPort);
                    OVXLink link = sw.getMap()
                            .getVirtualNetwork(sw.getTenantId())
                            .getLink(dstPort, inPort);
                    if (inPort != null && link != null) {
                        Integer flowId = sw
                                .getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getFlowManager()
                                .getFlowId(this.match.getDataLayerSource(),
                                        this.match.getDataLayerDestination());
                        OVXLinkUtils lUtils = new OVXLinkUtils(
                                sw.getTenantId(), link.getLinkId(), flowId);
                        lUtils.rewriteMatch(this.getMatch());
                    }
                }
            }
        } catch (NetworkMappingException e) {
            log.warn(
                    "OVXFlowMod. Error retrieving the network with id {} for flowMod {}. Dropping packet...",
                    this.sw.getTenantId(), this);
        } catch (DroppedMessageException e) {
            log.warn(
                    "OVXFlowMod. Error retrieving flowId in network with id {} for flowMod {}. Dropping packet...",
                    this.sw.getTenantId(), this);
        }
        this.computeLength();
        if (pflag) {
            this.flags |= OFFlowMod.OFPFF_SEND_FLOW_REM;
            sw.sendSouth(this, inPort);
        }
    }

    private void computeLength() {
        this.setActions(this.approvedActions);
        this.setLengthU(OVXFlowMod.MINIMUM_LENGTH);
        for (final OFAction act : this.approvedActions) {
            this.setLengthU(this.getLengthU() + act.getLengthU());
        }
    }

    private void prependRewriteActions() {
        if (!this.match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
            final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
            srcAct.setNetworkAddress(IPMapper.getPhysicalIp(sw.getTenantId(),
                    this.match.getNetworkSource()));
            this.approvedActions.add(0, srcAct);
        }

        if (!this.match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
            final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
            dstAct.setNetworkAddress(IPMapper.getPhysicalIp(sw.getTenantId(),
                    this.match.getNetworkDestination()));
            this.approvedActions.add(0, dstAct);
        }
    }

    /**
     * @param flagbit
     *            The OFFlowMod flag
     * @return true if the flag is set
     */
    public boolean hasFlag(short flagbit) {
        return (this.flags & flagbit) == flagbit;
    }

    public OVXFlowMod clone() {
        OVXFlowMod flowMod = null;
        try {
            flowMod = (OVXFlowMod) super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Error cloning flowMod: {}", this);
        }
        return flowMod;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (this.match != null) {
            map.put("match", new OVXMatch(match).toMap());
        }
        LinkedList<Map<String, Object>> actions = new LinkedList<Map<String, Object>>();
        for (OFAction act : this.actions) {
            try {
                actions.add(OVXUtil.actionToMap(act));
            } catch (UnknownActionException e) {
                log.warn("Ignoring action {} because {}", act, e.getMessage());
            }
        }
        map.put("actionsList", actions);
        map.put("priority", String.valueOf(this.priority));
        return map;
    }

    public void setVirtualCookie() {
        long tmp = this.ovxCookie;
        this.ovxCookie = this.cookie;
        this.cookie = tmp;
    }


}
