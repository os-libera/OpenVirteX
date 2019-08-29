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
package net.onrc.openvirtex.messages.actions;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.*;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.routing.SwitchRoute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U64;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OVXActionOutput extends OVXAction implements VirtualizableAction {
    Logger log = LogManager.getLogger(OVXActionOutput.class.getName());

    private OFActionOutput ofActionOutput;

    public OVXActionOutput(OFAction ofAction) {
        super(ofAction);
        this.ofActionOutput = (OFActionOutput)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {
        OFFactory ofFactory = OFFactories.getFactory(sw.getOfVersion());
        //this.log.info("virtualize");

        //final OVXPort2 inPort = sw.getPort(match.getMatch().get(MatchField.IN_PORT).getShortPortNumber());

        OVXPort inPort;

        LinkedList<OVXPort> outPortList;

        if(match.getMatch().get(MatchField.IN_PORT) != null) {
            inPort = sw.getPort(match.getMatch().get(MatchField.IN_PORT).getShortPortNumber());

            outPortList = this.fillPortList(
                    match.getMatch().get(MatchField.IN_PORT).getShortPortNumber(),
                    this.ofActionOutput.getPort().getShortPortNumber(),
                    sw
            );
        }else{
            inPort = sw.getPort((short)0);

            outPortList = this.fillPortList(
                    (short)0,
                    this.ofActionOutput.getPort().getShortPortNumber(),
                    sw
            );
        }

        // TODO: handle TABLE output port here
        final OVXNetwork vnet;
        try {
            vnet = sw.getMap().getVirtualNetwork(sw.getTenantId());
        } catch (NetworkMappingException e) {
            log.warn("{}: skipping processing of OFAction", e);
            return;
        }

        if (match.isFlowMod()) {
            final OVXFlowMod fm;
            try {
                fm = sw.getFlowMod(match.getCookie());
            } catch (MappingException e) {
                log.warn("FlowMod not found in our FlowTable");
                return;
            }

            fm.setOFMessage(fm.getFlowMod().createBuilder()
                    .setCookie(U64.of(match.getCookie()))
                    .build()
            );

            for (final OVXPort outPort : outPortList) {
                Integer linkId = 0;
                Integer flowId = 0;

                if (sw instanceof OVXBigSwitch
                        && inPort.getPhysicalPort().getParentSwitch()
                        != outPort.getPhysicalPort().getParentSwitch()) {
                    // Retrieve the route between the two OVXPorts
                    final OVXBigSwitch bigSwitch = (OVXBigSwitch) outPort.getParentSwitch();
                    final SwitchRoute route = bigSwitch.getRoute(inPort, outPort);
                    if (route == null) {
                        this.log.error(
                                "Cannot retrieve the bigswitch internal route between ports {} {}, dropping message",
                                inPort, outPort);
                        throw new DroppedMessageException(
                                "No such internal route");
                    }

                    // If the inPort belongs to an OVXLink, add rewrite actions
                    // to unset the packet link fields
                    if (inPort.isLink()) {
                        final OVXPort dstPort = vnet.getNeighborPort(inPort);
                        final OVXLink link = inPort.getLink().getOutLink();
                        if (link != null
                                && match.getMatch().get(MatchField.ETH_DST) != null
                                && match.getMatch().get(MatchField.ETH_SRC) != null) {
                            try {
                                flowId = vnet.getFlowManager()
                                        .getFlowId(
                                                match.getMatch().get(MatchField.ETH_SRC).getBytes(),
                                                match.getMatch().get(MatchField.ETH_DST).getBytes()
                                        );
                                OVXLinkUtils lUtils = new OVXLinkUtils(
                                        sw.getTenantId(), link.getLinkId(), flowId);
                                approvedActions.addAll(
                                        lUtils.unsetLinkFields(false, false, sw.getOfVersion())
                                );
                            } catch (IndexOutOfBoundException e) {
                                log.error(
                                        "Too many host to generate the flow pairs in this virtual network {}. "
                                                + "Dropping flow-mod {} ",
                                        sw.getTenantId(), fm);
                                throw new DroppedMessageException();
                            }
                        } else {
                            this.log.error(
                                    "Cannot retrieve the virtual link between ports {} {}, dropping message",
                                    dstPort, inPort);
                            return;
                        }
                    }


                    route.generateRouteFMs(fm.clone());


                    // add the output action with the physical outPort (srcPort
                    // of the route)
                    if (inPort.getPhysicalPortNumber() != route.getPathSrcPort().getPortNumber()) {
                        approvedActions.add(
                                ofFactory.actions().buildOutput()
                                        .setPort(OFPort.of(route.getPathSrcPort().getPortNumber()))
                                        .build()
                        );
                    } else {
                        approvedActions.add(
                                ofFactory.actions().buildOutput()
                                        .setPort(OFPort.IN_PORT)
                                        .build()
                        );
                    }
                }else {
                    /*
                     * SingleSwitch and BigSwitch with inPort & outPort
                     * belonging to the same physical switch
                     */
                    if (inPort.isEdge()) {
                        if (outPort.isEdge()) {
                            // TODO: this is logically incorrect, i have to do
                            // this because we always add the rewriting actions
                            // in the flowMod. Change it.
                            //log.info("prependUnRewriteActions1");
                            approvedActions.addAll(
                                    IPMapper.prependUnRewriteActions(match.getMatch())
                            );
                        } else {
                            /*
                             * If inPort is edge and outPort is link:
                             * - retrieve link
                             * - generate the link's FMs
                             * - add actions to current FM to write packet fields
                             * related to the link
                             */
                            final OVXLink link = outPort.getLink().getOutLink();
                            linkId = link.getLinkId();
                            try {
                                flowId = vnet.getFlowManager().storeFlowValues(
                                        match.getMatch().get(MatchField.ETH_SRC).getBytes(),
                                        match.getMatch().get(MatchField.ETH_DST).getBytes());
                                link.generateLinkFMs(fm.clone(), flowId);
                                approvedActions.addAll(new OVXLinkUtils(sw.getTenantId(), linkId, flowId)
                                        .setLinkFields(sw.getOfVersion()));
                            } catch (IndexOutOfBoundException e) {
                                log.error(
                                        "Too many host to generate the flow pairs in this virtual network {}. "
                                                + "Dropping flow-mod {} ",
                                        sw.getTenantId(), fm);
                                throw new DroppedMessageException();
                            }
                        }
                    } else {
                        if (outPort.isEdge()) {
                            /*
                             * If inPort belongs to a link and outPort is edge:
                             * - retrieve link
                             * - add actions to current FM to restore original IPs
                             * - add actions to current FM to restore packet fields
                             * related to the link
                             */
                            //log.info("prependUnRewriteActions2");
                            approvedActions.addAll(
                                    IPMapper.prependUnRewriteActions(match.getMatch())
                            );
                            // rewrite the OFMatch with the values of the link
                            final OVXPort dstPort = vnet
                                    .getNeighborPort(inPort);
                            final OVXLink link = dstPort.getLink().getOutLink();
                            if (link != null) {
                                try {
                                    flowId = vnet.getFlowManager().getFlowId(
                                            match.getMatch().get(MatchField.ETH_SRC).getBytes(),
                                            match.getMatch().get(MatchField.ETH_DST).getBytes());
                                    OVXLinkUtils lUtils = new OVXLinkUtils(
                                            sw.getTenantId(), link.getLinkId(),
                                            flowId);
                                    // Don't rewrite src or dst MAC if the action already exists
                                    // OFActionOutput만 있는 경우에도 이부분이 실행되나?
                                    boolean skipSrcMac = false;
                                    boolean skipDstMac = false;

                                    for (final OFAction act : approvedActions) {
                                        if(act.getVersion() == OFVersion.OF_10) {
                                            if (act instanceof OFActionSetDlSrc) {
                                                skipSrcMac = true;
                                            }
                                            if (act instanceof OFActionSetDlDst) {
                                                skipDstMac = true;
                                            }
                                        }else {
                                            if (act instanceof OFActionSetField) {
                                                if (((OFActionSetField) act).getField() == MatchField.ETH_SRC) {
                                                    skipSrcMac = true;
                                                }

                                                if (((OFActionSetField) act).getField() == MatchField.ETH_DST) {
                                                    skipDstMac = true;
                                                }
                                            }
                                        }
                                    }


                                    approvedActions.addAll(
                                            lUtils.unsetLinkFields(skipSrcMac, skipDstMac, sw.getOfVersion())
                                    );
                                } catch (IndexOutOfBoundException e) {
                                    log.error(
                                            "Too many host to generate the flow pairs in this virtual network {}. "
                                                    + "Dropping flow-mod {} ",
                                            sw.getTenantId(), fm);
                                    throw new DroppedMessageException();
                                }
                            } else {
                                // TODO: substitute all the return with
                                // exceptions
                                this.log.error(
                                        "Cannot retrieve the virtual link between ports {} {}, dropping message",
                                        dstPort, inPort);
                                return;
                            }
                        } else {
                            final OVXLink link = outPort.getLink().getOutLink();
                            linkId = link.getLinkId();
                            try {
                                flowId = vnet.getFlowManager().storeFlowValues(
                                        match.getMatch().get(MatchField.ETH_SRC).getBytes(),
                                        match.getMatch().get(MatchField.ETH_DST).getBytes());
                                link.generateLinkFMs(fm.clone(), flowId);
                                approvedActions.addAll(new OVXLinkUtils(sw
                                        .getTenantId(), linkId, flowId)
                                        .setLinkFields(sw.getOfVersion()));

                            } catch (IndexOutOfBoundException e) {
                                log.error(
                                        "Too many host to generate the flow pairs in this virtual network {}. "
                                                + "Dropping flow-mod {} ",
                                        sw.getTenantId(), fm);
                                throw new DroppedMessageException();
                            }
                        }
                    }
                    if (inPort.getPhysicalPortNumber() != outPort.getPhysicalPortNumber()) {
                        approvedActions.add(
                                ofFactory.actions().buildOutput()
                                        .setPort(OFPort.of(outPort.getPhysicalPortNumber()))
                                        .build()
                        );
                    } else {
                        approvedActions.add(
                                ofFactory.actions().buildOutput()
                                        .setPort(OFPort.IN_PORT)
                                        .build()
                        );
                    }
                }
            }
        }else if (match.isPacketOut()) {
            boolean throwException = true;

            for (final OVXPort outPort : outPortList) {

                if (outPort.isLink()) {
                    final OVXPort dstPort = outPort.getLink().getOutLink()
                            .getDstPort();

                    dstPort.getParentSwitch().sendMsg(
                            new OVXPacketIn(match.getPktData(),
                                    dstPort.getPortNumber(), sw.getOfVersion()), sw);

                    this.log.debug(
                            "Generate a packetIn from OVX Port {}/{}, physicalPort {}/{}",
                            dstPort.getParentSwitch().getSwitchName(),
                            dstPort.getPortNumber(), dstPort.getPhysicalPort()
                                    .getParentSwitch().getSwitchName(),
                            dstPort.getPhysicalPortNumber());
                }else if (sw instanceof OVXBigSwitch) {
                    /**
                     * Big-switch management. Generate a packetOut to the
                     * physical outPort
                     */
                    // Only generate pkt_out if a route is configured between in
                    // and output port.
                    // If parent switches are identical, no route will be configured
                    // although we do want to output the pkt_out.
                    if ((inPort == null)
                            || (inPort.getParentSwitch() == outPort.getParentSwitch())
                            || (((OVXBigSwitch) sw).getRoute(inPort, outPort) != null)) {
                        final PhysicalPort dstPort = outPort.getPhysicalPort();
                        dstPort.getParentSwitch().sendMsg(
                                new OVXPacketOut(match.getPktData(),
                                        OFPort.ANY.getShortPortNumber(),
                                        dstPort.getPortNumber(), sw.getOfVersion()), sw);
                        this.log.debug("PacketOut for a bigSwitch port, "
                                        + "generate a packet from Physical Port {}/{}",
                                dstPort.getParentSwitch().getSwitchName(),
                                dstPort.getPortNumber());
                    }
                } else {
                    /**
                     * Else (e.g. the outPort is an edgePort in a single switch)
                     * modify the packet and send to the physical switch.
                     */
                    throwException = false;
                    //log.info("prependUnRewriteActions3");
                    approvedActions.addAll(
                            IPMapper.prependUnRewriteActions(match.getMatch())
                    );

                    OFAction tempAction = ofFactory.actions().buildOutput()
                            .setPort(OFPort.of(outPort.getPhysicalPortNumber()))
                            .build();

                    approvedActions.add(tempAction);

                    this.log.debug(
                            "Physical ports are on the same physical switch, rewrite only outPort to {}",
                            outPort.getPhysicalPortNumber());
                }

            }

            if (throwException) {
                throw new DroppedMessageException();
            }
        }
    }

    private LinkedList<OVXPort> fillPortList(final Short inPort,
                                             final Short outPort, final OVXSwitch sw)
            throws DroppedMessageException {

        final LinkedList<OVXPort> outPortList = new LinkedList<OVXPort>();
        if (U16.f(outPort) < U16.f(OFPort.MAX.getShortPortNumber())) {
            if (sw.getPort(outPort) != null && sw.getPort(outPort).isActive()) {
                outPortList.add(sw.getPort(outPort));
            }
        } else if (U16.f(outPort) == U16.f(OFPort.FLOOD.getShortPortNumber())) {
            final Map<Short, OVXPort> ports = sw.getPorts();
            for (final OVXPort port : ports.values()) {
                if (port.getPortNumber() != inPort && port.isActive()) {
                    outPortList.add(port);
                }
            }
        } else if (U16.f(outPort) == U16.f(OFPort.ALL.getShortPortNumber())) {
            final Map<Short, OVXPort> ports = sw.getPorts();
            for (final OVXPort port : ports.values()) {
                if (port.isActive()) {
                    outPortList.add(port);
                }
            }
        } else {
            log.debug(
                    "Output port from controller currently not supported. Short = {}, Exadecimal = 0x{}, {}",
                    U16.f(outPort),
                    Integer.toHexString(U16.f(outPort) & 0xffff),
                    OFPort.CONTROLLER);
        }

        if (outPortList.size() < 1) {
            throw new DroppedMessageException(
                    "No output ports defined; dropping");
        }
        return outPortList;
    }

    @Override
    public int hashCode() {
        return this.getAction().hashCode();
    }
}
