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

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.XidPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

import java.nio.ByteBuffer;


/**
 * Utility class for OVX messages. Implements methods
 * for creating error messages and transaction ID
 * mappings.
 */
public final class OVXMessageUtil {

    private static Logger log = LogManager.getLogger(OVXMessageUtil.class.getName());

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private OVXMessageUtil() {
    }

    /**
     * Makes an OpenFlow error message for a bad action and
     * given OpenFlow message.
     *
     * @param code the bad action code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OVXMessage makeError(final OFBadActionCode code,
                                      final OVXMessage msg) {


        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.getOFMessage().writeTo(buf);

        final OVXError err = new OVXError(OFFactories.getFactory(msg.getOFMessage().getVersion()).errorMsgs().buildBadActionErrorMsg()
                .setCode(code)
                .setData(OFErrorCauseData.of(buf.array(), msg.getOFMessage().getVersion()))
                .setXid(msg.getOFMessage().getXid())
                .build());

        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed flow mod and
     * given OpenFlow message.
     *
     * @param code the failed flow mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OVXMessage makeErrorMsg(final OFFlowModFailedCode code,
                                         final OVXMessage msg) {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.getOFMessage().writeTo(buf);

        final OVXError err = new OVXError(OFFactories.getFactory(msg.getOFMessage().getVersion()).errorMsgs().buildFlowModFailedErrorMsg()
                .setCode(code)
                .setData(OFErrorCauseData.of(buf.array(), msg.getOFMessage().getVersion()))
                .setXid(msg.getOFMessage().getXid())
                .build());


        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed port mod and
     * given OpenFlow message.
     *
     * @param code the failed port mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OVXMessage makeErrorMsg(final OFPortModFailedCode code,
                                         final OVXMessage msg) {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.getOFMessage().writeTo(buf);

        final OVXError err = new OVXError(
                OFFactories.getFactory(msg.getOFMessage().getVersion()).errorMsgs().buildPortModFailedErrorMsg()
                        .setCode(code)
                        .setData(OFErrorCauseData.of(buf.array(), msg.getOFMessage().getVersion()))
                        .setXid(msg.getOFMessage().getXid())
                        .build()
        );

        return err;
    }

    /**
     * Makes an OpenFlow error message for a bad request and
     * given OpenFlow message.
     *
     * @param code the bad request code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OVXMessage makeErrorMsg(final OFBadRequestCode code,
                                         final OVXMessage msg) {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.getOFMessage().writeTo(buf);

        final OVXError err = new OVXError(
                OFFactories.getFactory(msg.getOFMessage().getVersion()).errorMsgs().buildBadRequestErrorMsg()
                        .setCode(code)
                        .setData(OFErrorCauseData.of(buf.array(), msg.getOFMessage().getVersion()))
                        .setXid(msg.getOFMessage().getXid())
                        .build()
        );

        return err;
    }

    /**
     * Xid translation based on port for "accurate" translation with a specific
     * PhysicalSwitch.
     *
     * @param msg the OpenFlow message
     * @param inPort the virtual input port instance
     * @return the virtual switch
     */
    public static OVXSwitch translateXid(final OVXMessage msg, final OVXPort inPort) {
        final OVXSwitch vsw = inPort.getParentSwitch();
        final int xid = vsw.translate(msg, inPort);

        msg.setOFMessage(
                msg.getOFMessage().createBuilder()
                        .setXid(xid)
                        .build()
        );

        return vsw;
    }

    /**
     * Xid translation based on OVXSwitch, for cases where port cannot be
     * determined.
     *
     * @param msg the OpenFlow message
     * @param vsw the virtual switch instance
     * @return new Xid for msg
     */
    public static Integer translateXid(final OVXMessage msg, final OVXSwitch vsw) {
        // this returns the original XID for a BigSwitch
        final Integer xid = vsw.translate(msg, null);

        msg.setOFMessage(
                msg.getOFMessage().createBuilder()
                        .setXid(xid)
                        .build()
        );

        return xid;
    }

    /**
     * Translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * For when port is known.
     *
     * @param msg the OpenFlow message
     * @param inPort the virtual input port instance
     */
    public static void translateXidAndSend(final OVXMessage msg,
                                           final OVXPort inPort) {
        final OVXSwitch vsw = OVXMessageUtil.translateXid(msg, inPort);
        vsw.sendSouth(msg, inPort);
    }

    /**
     * Translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * For when port is not known.
     *
     * @param msg the OpenFlow message
     * @param vsw the virtual switch instance
     */
    public static void translateXidAndSend(final OVXMessage msg,
                                           final OVXSwitch vsw) {
        final int newXid = OVXMessageUtil.translateXid(msg, vsw);

        if (vsw instanceof OVXBigSwitch) {
            // no port info for BigSwitch, to all its PhysicalSwitches. Is this
            // ok?
            try {
                for (final PhysicalSwitch psw : vsw.getMap()
                        .getPhysicalSwitches(vsw)) {
                    final int xid = psw.translate(msg, vsw);

                    msg.setOFMessage(
                            msg.getOFMessage().createBuilder()
                                    .setXid(xid)
                                    .build()
                    );

                    psw.sendMsg(msg, vsw);

                    msg.setOFMessage(
                            msg.getOFMessage().createBuilder()
                                    .setXid(newXid)
                                    .build()
                    );
                }
            } catch (SwitchMappingException e) {
                log.error("Switch {} is not mapped to any physical switches.", vsw);
            }
        } else {
            vsw.sendSouth(msg, null);
        }
    }

    /**
     * Undoes the XID translation and returns the original virtual switch.
     *
     * @param msg the OpenFlow message
     * @param psw the physical switch
     * @return the virtual switch
     */
    public static OVXSwitch untranslateXid(final OVXMessage msg,
                                           final PhysicalSwitch psw) {
        final XidPair<OVXSwitch> pair = psw.untranslate(msg);
        if (pair == null) {
            return null;
        }

        msg.setOFMessage(
                msg.getOFMessage().createBuilder()
                        .setXid(pair.getXid())
                        .build()
        );

        return pair.getSwitch();
    }

    /**
     * Undoes the Xid translation and tries to send the resulting message to the
     * origin OVXSwitch.
     *
     * @param msg the OpenFlow message
     * @param psw the physical switch
     */
    public static void untranslateXidAndSend(final OVXMessage msg,
                                             final PhysicalSwitch psw) {
        final OVXSwitch vsw = OVXMessageUtil.untranslateXid(msg, psw);
        if (vsw == null) {
            log.error("Cound not untranslate XID for switch {}", psw);
            return;
        }
        vsw.sendMsg(msg, psw);
    }

    public static OVXMessage toOVXMessage(OFMessage omsg) {
        switch(omsg.getType()){
            case HELLO:
                return new OVXHello(omsg);
            case BARRIER_REPLY:
                return new OVXBarrierReply(omsg);
            case BARRIER_REQUEST:
                return new OVXBarrierRequest(omsg);
            case ECHO_REPLY:
                return new OVXEchoReply(omsg);
            case ECHO_REQUEST:
                return new OVXEchoRequest(omsg);
            case ERROR:
                return new OVXError(omsg);
            case FEATURES_REPLY:
                return new OVXFeaturesReply(omsg);
            case FEATURES_REQUEST:
                return new OVXFeaturesRequest(omsg);
            case FLOW_MOD:
                return new OVXFlowMod(omsg);
            case FLOW_REMOVED:
                return new OVXFlowRemoved(omsg);
            case PACKET_IN:
                return new OVXPacketIn(omsg);
            case PACKET_OUT:
                return new OVXPacketOut(omsg);
            case PORT_STATUS:
                return new OVXPortStatus(omsg);
            case PORT_MOD:
                return new OVXPortMod(omsg);
            case SET_CONFIG:
                return new OVXSetConfig(omsg);
            case STATS_REPLY:
                return new OVXStatisticsReply(omsg);
            case STATS_REQUEST:
                return new OVXStatisticsRequest(omsg);
            case QUEUE_GET_CONFIG_REPLY:
                return new OVXQueueGetConfigReply(omsg);
            case QUEUE_GET_CONFIG_REQUEST:
                return new OVXQueueGetConfigRequest(omsg);
            case GET_CONFIG_REPLY:
                return new OVXGetConfigReply(omsg);
            case GET_CONFIG_REQUEST:
                return new OVXGetConfigRequest(omsg);
            case ROLE_REQUEST:
                return new OVXRoleRequest(omsg);
            case ROLE_REPLY:
                return new OVXRoleReply(omsg);
            default:
                log.info("toOVXMessage " + omsg.toString());
                return new OVXMessage(omsg);
        }
    }

    public static Match loadFromPacket(final byte[] packetData, final short inputPort, OFVersion ofVersion) {
        //packetData은 PacketIn으로 올라온 패킷(Ethernet+IP/ICMP+TCP/UCP)형태임 여기서 MAC주소등등의 정보를 Match로 저장한다.
        short scratch;
        int transportOffset = 34;
        final ByteBuffer packetDataBB = ByteBuffer.wrap(packetData);

        final int limit = packetDataBB.limit();

        OFFactory ofFactory = OFFactories.getFactory(ofVersion);

        Match tempMatch = ofFactory.buildMatch().build();

        tempMatch = updateMatch(tempMatch,
                tempMatch.createBuilder()
                        .setExact(MatchField.IN_PORT, OFPort.of(inputPort))
                        .build());

        //log.info("1 = " + tempMatch.toString());

        assert limit >= 14;
        // dl dst

        byte[] dataLayerDestination = new byte[6];
        packetDataBB.get(dataLayerDestination);
        // dl src
        byte[] dataLayerSource = new byte[6];
        packetDataBB.get(dataLayerSource);
        // dl type
        short dataLayerType = packetDataBB.getShort();

        tempMatch = updateMatch(tempMatch,
                tempMatch.createBuilder()
                        .setExact(MatchField.ETH_SRC, MacAddress.of(dataLayerSource))
                        .setExact(MatchField.ETH_DST, MacAddress.of(dataLayerDestination))
                        .build());

//        log.info("2 = " + tempMatch.toString());

        short dataLayerVirtualLan;
        byte dataLayerVirtualLanPriorityCodePoint;

        if (dataLayerType != (short) 0x8100) { // need cast to avoid
            // signed
            // bug
            dataLayerVirtualLan = (short) 0xffff;
            dataLayerVirtualLanPriorityCodePoint = (byte) 0;
        } else {
            // has vlan tag
            scratch = packetDataBB.getShort();
            dataLayerVirtualLan = ((short) (0xfff & scratch));
            dataLayerVirtualLanPriorityCodePoint = (byte) ((0xe000 & scratch) >> 13);
            dataLayerType = packetDataBB.getShort();

            tempMatch = updateMatch(tempMatch,
                    tempMatch.createBuilder()
                            .setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofRawVid(dataLayerVirtualLan))
                            .setExact(MatchField.VLAN_PCP, VlanPcp.of(dataLayerVirtualLanPriorityCodePoint))
                            .build());
        }

        byte networkTypeOfService = 0;
        byte networkProtocol = 0;
        int networkSource;
        int networkDestination;
        int transportSource = 0;
        int transportDestination = 0;

        switch (dataLayerType) {
            case 0x0800:
                // ipv4
                // check packet length
                scratch = packetDataBB.get();
                scratch = (short) (0xf & scratch);
                transportOffset = packetDataBB.position() - 1 + scratch * 4;
                // nw tos (dscp)
                scratch = packetDataBB.get();
                networkTypeOfService = (byte) ((0xfc & scratch) >> 2);
                // nw protocol
                packetDataBB.position(packetDataBB.position() + 7);
                networkProtocol = packetDataBB.get();
                // nw src
                packetDataBB.position(packetDataBB.position() + 2);
                networkSource = packetDataBB.getInt();
                // nw dst
                networkDestination = packetDataBB.getInt();
                packetDataBB.position(transportOffset);

                tempMatch = updateMatch(tempMatch,
                        tempMatch.createBuilder()
                                .setExact(MatchField.ETH_TYPE, EthType.of(dataLayerType))
                                .setExact(MatchField.IPV4_SRC, IPv4Address.of(networkSource))
                                .setExact(MatchField.IPV4_DST, IPv4Address.of(networkDestination))
                                .setExact(MatchField.IP_DSCP, IpDscp.of(networkTypeOfService))
                                .build());

                //log.info("IPV4_SRC " + IPv4Address.of(networkSource).toString());
                //log.info("IPV4_DST " + IPv4Address.of(networkDestination).toString());


                break;
            case 0x0806:
                // arp
                final int arpPos = packetDataBB.position();
                // opcode
                scratch = packetDataBB.getShort(arpPos + 6);
                networkProtocol = (byte) (0xff & scratch);

                scratch = packetDataBB.getShort(arpPos + 2);
                // if ipv4 and addr len is 4
                if (scratch == 0x800 && packetDataBB.get(arpPos + 5) == 4) {
                    // nw src
                    networkSource = packetDataBB.getInt(arpPos + 14);
                    // nw dst
                    networkDestination = packetDataBB.getInt(arpPos + 24);
                } else {
                    networkSource = 0;
                    networkDestination = 0;
                }

                tempMatch = updateMatch(tempMatch,
                        tempMatch.createBuilder()
                                .setExact(MatchField.ETH_TYPE, EthType.of(dataLayerType))
                                .setExact(MatchField.ARP_SPA, IPv4Address.of(networkSource))
                                .setExact(MatchField.ARP_TPA, IPv4Address.of(networkDestination))
                                .build());

//                log.info("ARP_SPA " + IPv4Address.of(networkSource).toString());
//                log.info("ARP_TPA " + IPv4Address.of(networkDestination).toString());
                break;
        }

//        log.info("3 = " + tempMatch.toString());

        switch (networkProtocol) {
            case 0x01:
                // icmp
                // type
                transportSource = U8.f(packetDataBB.get());
                // code
                transportDestination = U8.f(packetDataBB.get());

                tempMatch = updateMatch(tempMatch,
                        tempMatch.createBuilder()
                                .setExact(MatchField.IP_PROTO,IpProtocol.of(networkProtocol))
                                .build());
                break;
            case 0x06:
                // tcp
                // tcp src
                transportSource = U16.f(packetDataBB.getShort());
                // tcp dest
                transportDestination = U16.f(packetDataBB.getShort());

                tempMatch = updateMatch(tempMatch,
                        tempMatch.createBuilder()
                                .setExact(MatchField.IP_PROTO,IpProtocol.of(networkProtocol))
                                .setExact(MatchField.TCP_SRC, TransportPort.of(transportSource))
                                .setExact(MatchField.TCP_DST, TransportPort.of(transportDestination))
                                .build());
                break;
            case 0x11:
                // udp
                // udp src
                transportSource = U16.f(packetDataBB.getShort());
                // udp dest
                transportDestination = U16.f(packetDataBB.getShort());

                //log.info("UDP SRC port = " + TransportPort.of(transportSource) + "[" + transportSource + "]");
                //log.info("UDP DST port = " + TransportPort.of(transportDestination) + "[" + transportDestination + "]");


                tempMatch = updateMatch(tempMatch,
                        tempMatch.createBuilder()
                                .setExact(MatchField.IP_PROTO,IpProtocol.of(networkProtocol))
                                .setExact(MatchField.TCP_SRC, TransportPort.of(transportSource))
                                .setExact(MatchField.TCP_DST, TransportPort.of(transportDestination))
                                .build());
                break;
        }

//        log.info("4 = " + tempMatch.toString());

       return tempMatch;
    }

    public static Match updateMatch(Match tmatch, Match omatch) {

        if(tmatch.getVersion() == OFVersion.OF_10) {
            return omatch;
        }else {
            Match.Builder mBuilder = tmatch.createBuilder();
            for (MatchField mf : tmatch.getMatchFields()) {
                mBuilder.setExact(mf, tmatch.get(mf));
            }

            for (MatchField mf : omatch.getMatchFields()) {
                mBuilder.setExact(mf, omatch.get(mf));
            }

            return mBuilder.build();
        }
    }
}
