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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;
import net.onrc.openvirtex.protocol.OVXMatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

public class OVXPacketOut extends OFPacketOut implements Devirtualizable {


    private final Logger log = LogManager.getLogger(OVXPacketOut.class
            .getName());
    private OFMatch match = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();

    @Override
    public void devirtualize(final OVXSwitch sw) {

        final OVXPort inport = sw.getPort(this.getInPort());
        OVXMatch ovxMatch = null;

        if (this.getBufferId() == OVXPacketOut.BUFFER_ID_NONE) {
            if (this.getPacketData().length <= 14) {
                this.log.error("PacketOut has no buffer or data {}; dropping",
                        this);
                sw.sendMsg(OVXMessageUtil.makeErrorMsg(
                        OFBadRequestCode.OFPBRC_BAD_LEN, this), sw);
                return;
            }
            this.match = new OFMatch().loadFromPacket(this.packetData,
                    this.inPort);
            ovxMatch = new OVXMatch(match);
            ovxMatch.setPktData(this.packetData);
        } else {
            final OVXPacketIn cause = sw.getFromBufferMap(this.bufferId);
            if (cause == null) {
                this.log.error(
                        "Unknown buffer id {} for virtual switch {}; dropping",
                        this.bufferId, sw);
                return;
            }

            this.match = new OFMatch().loadFromPacket(cause.getPacketData(),
                    this.inPort);
            this.setBufferId(cause.getBufferId());
            ovxMatch = new OVXMatch(match);
            ovxMatch.setPktData(cause.getPacketData());
            if (cause.getBufferId() == OVXPacketOut.BUFFER_ID_NONE) {
                this.setPacketData(cause.getPacketData());
                this.setLengthU(this.getLengthU() + this.packetData.length);
            }
        }

        for (final OFAction act : this.getActions()) {
            try {
                ((VirtualizableAction) act).virtualize(sw,
                        this.approvedActions, ovxMatch);

            } catch (final ActionVirtualizationDenied e) {
                this.log.warn("Action {} could not be virtualized; error: {}",
                        act, e.getMessage());
                sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
                return;
            } catch (final DroppedMessageException e) {
                this.log.debug("Dropping packetOut {}", this);
                return;
            }
        }

        if (U16.f(this.getInPort()) < U16.f(OFPort.OFPP_MAX.getValue())) {
            this.setInPort(inport.getPhysicalPortNumber());
        }
        this.prependRewriteActions(sw);
        this.setActions(this.approvedActions);
        this.setActionsLength((short) 0);
        this.setLengthU(OVXPacketOut.MINIMUM_LENGTH + this.packetData.length);
        for (final OFAction act : this.approvedActions) {
            this.setLengthU(this.getLengthU() + act.getLengthU());
            this.setActionsLength((short) (this.getActionsLength() + act
                    .getLength()));
        }

        // TODO: Beacon sometimes send msg with inPort == controller, check with
        // Ayaka if it's ok
        if (U16.f(this.getInPort()) < U16.f(OFPort.OFPP_MAX.getValue())) {
            OVXMessageUtil.translateXid(this, inport);
        }
        this.log.debug("Sending packet-out to sw {}: {}", sw.getName(), this);
        sw.sendSouth(this, inport);
    }

    private void prependRewriteActions(final OVXSwitch sw) {
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

    public OVXPacketOut(final OVXPacketOut pktOut) {
        this.bufferId = pktOut.bufferId;
        this.inPort = pktOut.inPort;
        this.length = pktOut.length;
        this.packetData = pktOut.packetData;
        this.type = pktOut.type;
        this.version = pktOut.version;
        this.xid = pktOut.xid;
        this.actions = pktOut.actions;
        this.actionsLength = pktOut.actionsLength;
    }

    public OVXPacketOut() {
        super();
    }

    public OVXPacketOut(final byte[] pktData, final short inPort,
            final short outPort) {
        this.setInPort(inPort);
        this.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        final OFActionOutput outAction = new OFActionOutput(outPort);
        final ArrayList<OFAction> actions = new ArrayList<OFAction>();
        actions.add(outAction);
        this.setActions(actions);
        this.setActionsLength(outAction.getLength());
        this.setPacketData(pktData);
        this.setLengthU((short) (OFPacketOut.MINIMUM_LENGTH
                + this.getPacketData().length + OFActionOutput.MINIMUM_LENGTH));
    }


}
