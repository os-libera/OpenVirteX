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

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.actions.OVXAction;
import net.onrc.openvirtex.messages.actions.OVXActionUtil;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.projectfloodlight.openflow.util.HexString;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OVXPacketOut extends OVXMessage implements Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXPacketOut.class
            .getName());
    private Match match = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();

    public OVXPacketOut(OFMessage msg) {
        super(msg);
    }

    public OVXPacketOut(final byte[] pktData, final short inPort,
                        final short outPort, OFVersion ofVersion) {

        super(null);

        OFActions ofActions = OFFactories.getFactory(ofVersion).actions();

        final ArrayList<OFAction> actions = new ArrayList<OFAction>();

        OFActionOutput actionOutput = ofActions.buildOutput()
                .setPort(OFPort.of(outPort))
                .setMaxLen((short) 65535)
                .build();

        actions.add(actionOutput);


        this.setOFMessage(OFFactories.getFactory(ofVersion).buildPacketOut()
                .setInPort(OFPort.of(inPort))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                .setData(pktData)
                .build()
        );
    }

    public OFPacketOut getPacketOut() {
        return (OFPacketOut)this.getOFMessage();
    }

    @Override
    public void devirtualize(final OVXSwitch sw) throws OFParseError {
        this.log.debug("devirtualize");
        //this.log.info(HexString.toHexString(this.getPacketOut().getData()));

        OVXPort inport = sw.getPort(this.getPacketOut().getInPort().getShortPortNumber());

        OVXMatch ovxMatch = null;

        if (this.getPacketOut().getBufferId() == OFBufferId.NO_BUFFER) {

            if (this.getPacketOut().getData().length <= 14) {
                this.log.error("PacketOut has no buffer or data {}; dropping",
                        this);
                sw.sendMsg(OVXMessageUtil.makeErrorMsg(OFBadRequestCode.BAD_LEN, this), sw);
                return;
            }

            this.match = OVXMessageUtil.loadFromPacket(
                    this.getPacketOut().getData(),
                    this.getPacketOut().getInPort().getShortPortNumber(),
                    sw.getOfVersion()
            );

            this.log.debug("Data Length = " + this.getPacketOut().getData().length);
            this.log.debug(this.match.toString());

            ovxMatch = new OVXMatch(this.match);
            ovxMatch.setPktData(this.getPacketOut().getData());
        } else {

            final OVXPacketIn cause = sw.getFromBufferMap(this.getPacketOut().getBufferId().getInt());

            if (cause == null) {
                this.log.error(
                        "Unknown buffer id {} for virtual switch {}; dropping",
                        this.getPacketOut().getBufferId().getInt(), sw);
                return;
            }

            this.match = OVXMessageUtil.loadFromPacket(
                    cause.getPacketIn().getData(),
                    this.getPacketOut().getInPort().getShortPortNumber(),
                    sw.getOfVersion()
            );

            this.setOFMessage(this.getPacketOut().createBuilder()
                    .setBufferId(cause.getPacketIn().getBufferId())
                    .build()
            );

            ovxMatch = new OVXMatch(this.match);
            ovxMatch.setPktData(cause.getPacketIn().getData());

            if (cause.getPacketIn().getBufferId() == OFBufferId.NO_BUFFER) {

                this.setOFMessage(this.getPacketOut().createBuilder()
                        .setData(cause.getPacketIn().getData())
                        .build()
                );
            }
        }

        for (final OFAction act : this.getPacketOut().getActions()) {
            try {
                OVXAction action2 = OVXActionUtil.wrappingOVXAction(act);

                ((VirtualizableAction) action2).virtualize(sw, this.approvedActions, ovxMatch);

            } catch (final ActionVirtualizationDenied e) {
                this.log.warn("Action {} could not be virtualized; error: {}",
                        act, e.getMessage());
                sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
                return;
            } catch (final DroppedMessageException e) {
                this.log.debug("Dropping packetOut {}", this);
                return;
            } catch (final NullPointerException e) {
                this.log.debug("Action {} could not be supported", act);
                return;
            }
        }

        if (U16.f(this.getPacketOut().getInPort().getShortPortNumber()) <
                U16.f(OFPort.MAX.getShortPortNumber())) {
            this.setOFMessage(this.getPacketOut().createBuilder()
                    .setInPort(OFPort.of(inport.getPhysicalPortNumber()))
                    .build()
            );
        }

        this.prependRewriteActions(sw);

        this.setOFMessage(this.getPacketOut().createBuilder()
                .setActions(this.approvedActions)
                .build()
        );

        if (U16.f(this.getPacketOut().getInPort().getShortPortNumber()) <
                U16.f(OFPort.MAX.getShortPortNumber())) {
            OVXMessageUtil.translateXid(this, inport);
        }
        this.log.debug("Sending packet-out to sw {}: {}", sw.getName(), this);
        //this.log.info(HexString.toHexString(this.getPacketOut().getData()));
        sw.sendSouth(this, inport);
    }

    private void prependRewriteActions(final OVXSwitch sw) {
        if(this.getOFMessage().getVersion() == OFVersion.OF_10)
            prependRewriteActionsVer10(sw);
        else
            prependRewriteActionsVer13(sw);
    }

    private void prependRewriteActionsVer13(final OVXSwitch sw) {
        if(this.match.get(MatchField.IPV4_SRC) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Src(
                            IPv4Address.of(
                                    IPMapper.getPhysicalIp(
                                            sw.getTenantId(),
                                            this.match.get(MatchField.IPV4_SRC).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }

        if(this.match.get(MatchField.IPV4_DST) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Dst(
                            IPv4Address.of(
                                    IPMapper.getPhysicalIp(
                                            sw.getTenantId(),
                                            this.match.get(MatchField.IPV4_DST).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }
    }

    private void prependRewriteActionsVer10(final OVXSwitch sw) {
        if(this.match.get(MatchField.IPV4_SRC) != null) {
            OFActionSetNwSrc srcAct = this.factory.actions().buildSetNwSrc()
                    .setNwAddr(IPv4Address.of(IPMapper.getPhysicalIp(sw.getTenantId(),
                            this.match.get(MatchField.IPV4_SRC).getInt())))
                    .build();
            this.approvedActions.add(0, srcAct);
        }

        if(this.match.get(MatchField.IPV4_DST) != null) {
            OFActionSetNwDst dstAct = this.factory.actions().buildSetNwDst()
                    .setNwAddr(IPv4Address.of(IPMapper.getPhysicalIp(sw.getTenantId(),
                            this.match.get(MatchField.IPV4_DST).getInt())))
                    .build();
            this.approvedActions.add(0, dstAct);
        }
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
