/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.messages;

import java.util.*;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.FlowTable;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.*;
import net.onrc.openvirtex.messages.actions.*;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.OVXUtil;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

public class OVXFlowMod extends OVXMessage implements Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXFlowMod.class.getName());

    private OVXSwitch sw = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();

    private long ovxCookie = -1;

    public OVXFlowMod(OFMessage msg) {
        super(msg);
    }

    public OFFlowMod getFlowMod() {
        return (OFFlowMod)this.getOFMessage();
    }

    @Override
    public void devirtualize(final OVXSwitch sw) {
        //this.log.info("devirtualize");
        //this.log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        //this.log.info(this.getFlowMod().toString());
        //this.log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        //this.log.info(this.getOFMessage().toString());



         /* Drop LLDP-matching messages sent by some applications */
        if (this.getFlowMod().getMatch().get(MatchField.ETH_TYPE) == EthType.LLDP) {
            return;
        }

        this.sw = sw;
        FlowTable ft = this.sw.getFlowTable();

        int bufferId = OFBufferId.NO_BUFFER.getInt();
        if (sw.getFromBufferMap(this.getFlowMod().getBufferId().getInt()) != null) {
            bufferId = ((OFPacketIn)sw.getFromBufferMap(this.getFlowMod().getBufferId().getInt()).getOFMessage())
                    .getBufferId().getInt();
        }
        //OFMatch에서 inport의 기본값은 0으로 설정되기 때문, 그러나 OpenFlowj에서는 MatchField가 존재하지 않으면
        //필드 자체가 없기 때문에 inport값을 알 수 없다.
        //ONOS인 경우 스위치가 연결되면 기본적인 설정 FlowMod 메시지를 보낸다(ARP, LLDP, IPv4정보를 Controller로 보내는 설정)
        //거기엔 in_port 정보가 없다 추후 이부분의 루틴 구현해야함

        /*if(!((OFFlowMod)this.getOFMessage()).getMatch().isExact(MatchField.IN_PORT)) {
            this.log.info("No IN_PORT in MatchField");
            return;
        }*/

        short inport = 0;

        if(this.getFlowMod().getMatch().get(MatchField.IN_PORT) != null)
        {
           inport = this.getFlowMod().getMatch()
                   .get(MatchField.IN_PORT).getShortPortNumber();
        }
        boolean pflag = ft.handleFlowMods(this.clone());


        //((OVXFlowTable) ft).dump();


        OVXMatch ovxMatch = new OVXMatch(this.getFlowMod().getMatch());
        ovxCookie = ((OVXFlowTable) ft).getCookie(this, false);

        ovxMatch.setCookie(ovxCookie);

        this.setOFMessage(this.getFlowMod().createBuilder()
                .setCookie(U64.of(ovxMatch.getCookie()))
                .build()
        );

        for (final OFAction act : this.getFlowMod().getActions()) {
            try {
                OVXAction action2 = OVXActionUtil.wrappingOVXAction(act);

                ((VirtualizableAction) action2).virtualize(sw, this.approvedActions, ovxMatch);

            } catch (final ActionVirtualizationDenied e) {
                this.log.debug("Action {} could not be virtualized; error: {}",
                        act, e.getMessage());
                ft.deleteFlowMod(ovxCookie);
                sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
                return;
            } catch (final DroppedMessageException e) {
                this.log.debug("Dropping ovxFlowMod {} {}", this.getOFMessage().toString(), e);
                ft.deleteFlowMod(ovxCookie);
                // TODO perhaps send error message to controller
                return;
            } catch (final NullPointerException e) {
                this.log.debug("Action {} could not be supported", act);
                return;
            }
        }

        final OVXPort ovxInPort = sw.getPort(inport);

        this.setOFMessage(this.getFlowMod().createBuilder()
                .setBufferId(OFBufferId.of(bufferId))
                .build()
        );

        if (ovxInPort == null) {
            if(this.getFlowMod().getMatch().isFullyWildcarded(MatchField.IN_PORT)) {

                for (OVXPort iport : sw.getPorts().values()) {
                    prepAndSendSouth(iport, pflag);
                }
            } else {
                this.log.error(
                        "Unknown virtual port id {}; dropping ovxFlowMod {}",
                        inport, this);
                sw.sendMsg(OVXMessageUtil.makeErrorMsg(OFFlowModFailedCode.EPERM, this), sw);
                return;
            }
        } else {
            prepAndSendSouth(ovxInPort, pflag);
        }

    }

    public void modifyMatch(Match match)
    {
        this.setOFMessage(this.getFlowMod().createBuilder()
                .setMatch(match)
                .build()
        );
    }

    private void prepAndSendSouth(OVXPort inPort, boolean pflag) {
        if (!inPort.isActive()) {
            log.warn("Virtual network {}: port {} on switch {} is down.",
                    sw.getTenantId(), inPort.getPortNumber(),
                    sw.getSwitchName());
            return;
        }

        this.modifyMatch(
                OVXMessageUtil.updateMatch(
                        this.getFlowMod().getMatch(),
                        this.getFlowMod().getMatch().createBuilder()
                                .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                .build()
                )
        );

        OVXMessageUtil.translateXid(this, inPort);
        try {
            if (inPort.isEdge()) {
                this.prependRewriteActions();
            } else {

                this.modifyMatch(
                        IPMapper.rewriteMatch(
                                sw.getTenantId(),
                                this.getFlowMod().getMatch()
                        )
                );

                // TODO: Verify why we have two send points... and if this is
                // the right place for the match rewriting
                if (inPort != null
                        && inPort.isLink()
                        && this.getFlowMod().getMatch().get(MatchField.ETH_DST) != null
                        && this.getFlowMod().getMatch().get(MatchField.ETH_SRC) != null
                ) {
                    // rewrite the OFMatch with the values of the link
                    OVXPort dstPort = sw.getMap()
                            .getVirtualNetwork(sw.getTenantId())
                            .getNeighborPort(inPort);

                    OVXLink link = sw.getMap()
                            .getVirtualNetwork(sw.getTenantId())
                            .getLink(dstPort, inPort);

                    if (inPort != null && link != null) {
                        try {

                            Integer flowId = sw.getMap()
                                    .getVirtualNetwork(sw.getTenantId())
                                    .getFlowManager()
                                    .getFlowId(
                                            this.getFlowMod().getMatch().get(MatchField.ETH_SRC).getBytes(),
                                            this.getFlowMod().getMatch().get(MatchField.ETH_DST).getBytes()
                                    );


                            OVXLinkUtils lUtils = new OVXLinkUtils(
                                    sw.getTenantId(), link.getLinkId(), flowId);

                            this.log.debug("before " + this.getFlowMod().getMatch().toString());

                            this.modifyMatch(
                                    lUtils.rewriteMatch(this.getFlowMod().getMatch())
                            );

                            this.log.debug("after " + this.getFlowMod().getMatch().toString());
                        } catch (IndexOutOfBoundException e) {
                            log.error(
                                    "Too many host to generate the flow pairs in this virtual network {}. "
                                            + "Dropping flow-mod {} ",
                                    sw.getTenantId(), this);
                            throw new DroppedMessageException();
                        }
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

        this.setOFMessage(this.getFlowMod().createBuilder()
                .setActions(this.approvedActions)
                .build()
        );

        if (pflag) {

            if(!this.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM))
                this.getFlowMod().getFlags().add(OFFlowModFlags.SEND_FLOW_REM);

            sw.sendSouth(this, inPort);
        }
    }

    private void prependRewriteActions() {
        if(this.getOFMessage().getVersion() == OFVersion.OF_10)
            prependRewriteActionsVer10();
        else
            prependRewriteActionsVer13();

    }

    private void prependRewriteActionsVer13() {
        if(this.getFlowMod().getMatch().get(MatchField.IPV4_SRC) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Src(IPv4Address.of(
                            IPMapper.getPhysicalIp(
                                    sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_SRC).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }

        if(this.getFlowMod().getMatch().get(MatchField.IPV4_DST) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Dst(IPv4Address.of(
                            IPMapper.getPhysicalIp(
                                    sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_DST).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }
    }

    private void prependRewriteActionsVer10() {
        if(this.getFlowMod().getMatch().get(MatchField.IPV4_SRC) != null) {
            OFAction action = this.factory.actions().buildSetNwSrc()
                            .setNwAddr(IPv4Address.of(
                                    IPMapper.getPhysicalIp(
                                            sw.getTenantId(),
                                            this.getFlowMod().getMatch().get(MatchField.IPV4_SRC).getInt())))
                            .build();

            this.approvedActions.add(0, action);
        }

        if(this.getFlowMod().getMatch().get(MatchField.IPV4_DST) != null) {
            OFAction action = this.factory.actions().buildSetNwDst()
                            .setNwAddr(IPv4Address.of(
                                    IPMapper.getPhysicalIp(sw.getTenantId(),
                                            this.getFlowMod().getMatch().get(MatchField.IPV4_DST).getInt())))
                            .build();

            this.approvedActions.add(0, action);
        }
    }

    public OVXFlowMod clone() {
        OVXFlowMod flowMod = new OVXFlowMod(this.getOFMessage().createBuilder().build());
        return flowMod;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (this.getFlowMod().getMatch() != null) {
            map.put("match", new OVXMatch(this.getFlowMod().getMatch()).toMap());
        }
        LinkedList<Map<String, Object>> actions = new LinkedList<Map<String, Object>>();
        for (OFAction act : this.getFlowMod().getActions()) {
            try {
                actions.add(OVXUtil.actionToMap(act));
            } catch (UnknownActionException e) {
                log.warn("Ignoring action {} because {}", act, e.getMessage());
            }
        }
        map.put("actionsList", actions);
        map.put("priority", String.valueOf(this.getFlowMod().getPriority()));
        return map;
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
