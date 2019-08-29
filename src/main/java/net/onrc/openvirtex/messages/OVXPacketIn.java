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

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkField;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.packet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.projectfloodlight.openflow.util.HexString;

public class OVXPacketIn extends OVXMessage implements Virtualizable {

    private final Logger log = LogManager.getLogger(OVXPacketIn.class.getName());
    private PhysicalPort port = null;
    private OVXPort ovxPort = null;
    private Integer tenantId = null;

    public OVXPacketIn(final OVXPacketIn pktIn) {

        super(pktIn.getOFMessage().createBuilder().build());
    }

    public OVXPacketIn(OFMessage msg) {

        super(msg);
    }

    public OVXPacketIn(final byte[] data, final short portNumber, OFVersion ofVersion) {
        super(null);

        OFFactory factory = OFFactories.getFactory(ofVersion);

        if(ofVersion == OFVersion.OF_10) {
            this.setOFMessage(factory.buildPacketIn()
                    .setInPort(OFPort.of(portNumber))
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .setData(data)
                    .build()
            );
        }else{
            Match match = factory.buildMatch()
                    .setExact(MatchField.IN_PORT, OFPort.of(portNumber))
                    .build();

            this.setOFMessage(factory.buildPacketIn()
                    .setMatch(match)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .setData(data)
                    .build()
            );
        }
    }

    public OFPacketIn getPacketIn() {
        return (OFPacketIn)this.getOFMessage();
    }

    public void setInport(short inport) {
        if(this.getOFMessage().getVersion() == OFVersion.OF_10) {
            this.setOFMessage(this.getPacketIn().createBuilder()
                    .setInPort(OFPort.of(inport))
                    .build()
            );
        }else{
            Match temp = this.getPacketIn().getMatch();
            temp = OVXMessageUtil.updateMatch(temp, temp.createBuilder()
                    .setExact(MatchField.IN_PORT, OFPort.of(inport))
                    .build());

            this.setOFMessage(
                    this.getPacketIn().createBuilder().setMatch(temp).build()
            );
        }
    }

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        //this.log.info("virtualize");
        //this.log.info(HexString.toHexString(this.getPacketIn().getData()));

        //OF_1.3일 경우 PACKET_IN으로 오는 데이터에 Ethernet Trailer가 붙어서 오는데 이 패킷을 OVX가
        // ONOS로 전송하면 ONOS는 그부분을 실제 데이터로 인식하여 UDP의 length로 포함되어 PACKET_OUT으로 내려보낸다.
        // 그러나 checksum은 그대로 이기 때문에 Destination에서 Receive를 하지 못하는 문제가 생긴다.
        // 이것을 해결하고자 Data에서 Trailer를 삭제하는 루틴을 구현한것
        Ethernet eth2 = new Ethernet();
        eth2.deserialize(this.getPacketIn().getData(), 0, this.getPacketIn().getData().length);

        if(eth2.getPayload() instanceof IPv4) {
            IPv4 ip = (IPv4)eth2.getPayload();
            if (ip.isTruncated()) {
                ByteBuffer bb = ByteBuffer.wrap(this.getPacketIn().getData());
                byte[] temp = new byte[bb.limit()-ip.getTrimLength()];
                bb.get(temp, 0, temp.length);
                this.setOFMessage(this.getPacketIn().createBuilder().setData(temp).build());
            }
        }

        OVXSwitch vSwitch = OVXMessageUtil.untranslateXid(this, sw);

        /*
         * Fetching port from the physical switch
         */

        short inport;// = ((OFPacketIn) this.getOFMessage()).getInPort().getShortPortNumber();
        if(this.getOFMessage().getVersion() == OFVersion.OF_10) {
            inport = this.getPacketIn().getInPort().getShortPortNumber();
        }else{
            if(this.getPacketIn().getMatch().get(MatchField.IN_PORT) != null)
                inport = this.getPacketIn().getMatch().get(MatchField.IN_PORT).getShortPortNumber();
            else
                inport = 0;
        }

        this.log.debug("inport = " + inport);

        port = sw.getPort(inport);

        Mappable map = sw.getMap();

        Match match = OVXMessageUtil.loadFromPacket(
                this.getPacketIn().getData(),
                inport,
                sw.getOfVersion());

        this.log.debug(match.toString());

        if (this.port.isEdge()) {
            this.tenantId = this.fetchTenantId(match, map, true);

            if (this.tenantId == null) {
                this.log.debug(
                        "PacketIn {} does not belong to any virtual network; "
                                + "dropping and installing a temporary drop rule",
                        this);
                this.installDropRule(sw, match);
                return;
            }

            /*
             * Checks on vSwitch and the virtual port done in swndPkt.
             */
            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
            this.ovxPort = this.port.getOVXPort(this.tenantId, 0);
            this.sendPkt(vSwitch, match, sw);
            this.learnHostIP(match, map);
            this.learnAddresses(match);
            this.log.debug("Edge PacketIn {} sent to virtual network {}", this.getOFMessage().toString(),
                    this.tenantId);
            return;
        }

        if (match.get(MatchField.ETH_TYPE) == EthType.IPv4
                || match.get(MatchField.ETH_TYPE) == EthType.ARP) {

            PhysicalIPAddress srcIP = new PhysicalIPAddress(
                    match.get(MatchField.IPV4_SRC).getInt()
            );

            PhysicalIPAddress dstIP = new PhysicalIPAddress(
                    match.get(MatchField.IPV4_DST).getInt()
            );

            Ethernet eth = new Ethernet();
            eth.deserialize(this.getPacketIn().getData(), 0, this.getPacketIn().getData().length);

            OVXLinkUtils lUtils = new OVXLinkUtils(eth.getSourceMAC(),
                    eth.getDestinationMAC());

            if (lUtils.isValid()) {
                OVXPort srcPort = port.getOVXPort(lUtils.getTenantId(),
                        lUtils.getLinkId());

                if (srcPort == null) {
                    this.log.debug(
                            "Virtual Src Port Unknown: {}, port {} with this match {}; dropping packet",
                            sw.getName(), match.get(MatchField.IN_PORT).getShortPortNumber(), match);
                    return;
                }

                this.setInport(srcPort.getPortNumber());

                OVXLink link;
                try {
                    OVXPort dstPort = map.getVirtualNetwork(
                            lUtils.getTenantId()).getNeighborPort(srcPort);
                    link = map.getVirtualSwitch(sw, lUtils.getTenantId())
                            .getMap().getVirtualNetwork(lUtils.getTenantId())
                            .getLink(dstPort, srcPort);
                } catch (SwitchMappingException | NetworkMappingException e) {
                    return; // same as (link == null)
                }
                this.ovxPort = this.port.getOVXPort(lUtils.getTenantId(),
                        link.getLinkId());
                OVXLinkField linkField = OpenVirteXController.getInstance()
                        .getOvxLinkField();
                // TODO: Need to check that the values in linkId and flowId
                // don't exceed their space
                if (linkField == OVXLinkField.MAC_ADDRESS) {
                    try {
                        LinkedList<MacAddress> macList = sw.getMap()
                                .getVirtualNetwork(this.ovxPort.getTenantId())
                                .getFlowManager()
                                .getFlowValues(lUtils.getFlowId());
                        eth.setSourceMACAddress(macList.get(0).getBytes())
                                .setDestinationMACAddress(macList.get(1).getBytes());
                        match = OVXMessageUtil.updateMatch(match, match.createBuilder()
                                .setExact(MatchField.ETH_SRC, eth.getSourceMAC())
                                .setExact(MatchField.ETH_DST, eth.getDestinationMAC())
                                .build()
                        );

                    } catch (NetworkMappingException e) {
                        log.warn(e);
                    }
                } else if (linkField == OVXLinkField.VLAN) {
                    // TODO
                    log.warn("VLAN virtual links not yet implemented.");
                    return;
                }
            }

            if (match.get(MatchField.ETH_TYPE) == EthType.ARP) {
                // ARP packet
                final ARP arp = (ARP) eth.getPayload();
                this.tenantId = this.fetchTenantId(match, map, true);

                try {
                    if (map.hasVirtualIP(srcIP)) {
                        arp.setSenderProtocolAddress(map.getVirtualIP(srcIP)
                                .getIp());
                    }
                    if (map.hasVirtualIP(dstIP)) {
                        arp.setTargetProtocolAddress(map.getVirtualIP(dstIP)
                                .getIp());
                    }
                } catch (AddressMappingException e) {
                    log.warn("Inconsistency in OVXMap? : {}", e);
                }
            }else if (match.get(MatchField.ETH_TYPE) == EthType.IPv4) {
                try {
                    final IPv4 ip = (IPv4) eth.getPayload();
                    ip.setDestinationAddress(map.getVirtualIP(dstIP).getIp());
                    ip.setSourceAddress(map.getVirtualIP(srcIP).getIp());
                    // TODO: Incorporate below into fetchTenantId
                    if (this.tenantId == null) {
                        this.tenantId = dstIP.getTenantId();
                    }
                } catch (AddressMappingException e) {
                    log.warn("Could not rewrite IP fields : {}", e);
                }
            } else {
                this.log.info("{} handling not yet implemented; dropping",
                        match.get(MatchField.ETH_TYPE).toString());
                this.installDropRule(sw, match);
                return;
            }

            this.setOFMessage(this.getPacketIn().createBuilder()
                    .setData(eth.serialize())
                    .build()
            );

            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);

            this.sendPkt(vSwitch, match, sw);
            this.log.info("IPv4 PacketIn {} sent to virtual network {}", this,
                    this.tenantId);
            return;
        }

        this.tenantId = this.fetchTenantId(match, map, true);
        if (this.tenantId == null) {
            this.log.debug(
                    "PacketIn {} does not belong to any virtual network; "
                            + "dropping and installing a temporary drop rule",
                    this);
            this.installDropRule(sw, match);
            return;
        }
        vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);

        this.sendPkt(vSwitch, match, sw);
        this.log.info("Layer2 PacketIn {} sent to virtual network {}", this.getOFMessage(),
                this.tenantId);
    }

    private void learnHostIP(Match match, Mappable map) {
        if(match.get(MatchField.IPV4_SRC) != null) {
            try {
                OVXNetwork vnet = map.getVirtualNetwork(this.tenantId);
                Host host = vnet.getHost(ovxPort);
                if (host != null) {
                    host.setIPAddress(match.get(MatchField.IPV4_SRC).getInt());
                } else {
                    log.warn("Host not found on virtual port {}", ovxPort);
                }
            } catch (NetworkMappingException e) {
                log.warn("Failed to lookup virtual network {}", this.tenantId);
            } catch (NullPointerException npe) {
                log.warn("No host attached at {} port {}", this.ovxPort
                        .getParentSwitch().getSwitchName(), this.ovxPort
                        .getPhysicalPortNumber());
            }
        }
    }

    private void sendPkt(final OVXSwitch vSwitch, final Match match,
                         final PhysicalSwitch sw) {
        if (vSwitch == null || !vSwitch.isActive()) {
            this.log.warn(
                    "Controller for virtual network {} has not yet connected "
                            + "or is down", this.tenantId);
            this.installDropRule(sw, match);
            return;
        }

        this.setOFMessage(this.getPacketIn().createBuilder()
                .setBufferId(OFBufferId.of(vSwitch.addToBufferMap(this)))
                .build()
        );

        short inport;
        if(this.getOFMessage().getVersion() == OFVersion.OF_10) {
            inport = this.getPacketIn().getInPort().getShortPortNumber();
        }else{
            if(this.getPacketIn().getMatch().get(MatchField.IN_PORT) != null)
                inport = this.getPacketIn().getMatch().get(MatchField.IN_PORT).getShortPortNumber();
            else
                inport = 0;
        }

        if (this.port != null && this.ovxPort != null
                && this.ovxPort.isActive()) {

            //this.log.info("before-----------------------------------------------------");
            //this.log.info(HexString.toHexString(this.getPacketIn().getData()));

            this.setInport(this.ovxPort.getPortNumber());

            if((this.getPacketIn().getData() != null)
                && (vSwitch.getMissSendLen() != OVXSetConfig.MSL_FULL)) {

                this.setOFMessage(this.getPacketIn().createBuilder()
                        .setData(Arrays.copyOf(
                                this.getPacketIn().getData(),
                                U16.f(vSwitch.getMissSendLen())))
                        .build()
                );
            }

            //this.log.info("after------------------------------------------------------");
            //this.log.info(HexString.toHexString(this.getPacketIn().getData()));

            vSwitch.sendMsg(this, sw);
        }else if (this.port == null) {
            log.error("The port {} doesn't belong to the physical switch {}", inport, sw.getName());
        }else if (this.ovxPort == null || !this.ovxPort.isActive()) {
            log.error(
                    "Virtual port associated to physical port {} in physical switch {} for "
                            + "virtual network {} is not defined or inactive",
                    inport, sw.getName(), this.tenantId);
        }
    }



    private void learnAddresses(final Match match) {
        if(match.get(MatchField.ETH_TYPE) == EthType.IPv4
            || match.get(MatchField.ETH_TYPE) == EthType.ARP) {
            if(match.get(MatchField.IPV4_SRC) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.IPV4_SRC).getInt());
            }

            if(match.get(MatchField.IPV4_DST) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.IPV4_DST).getInt());
            }
        }
    }

    private void installDropRule(final PhysicalSwitch sw, final Match match) {
        final OVXFlowMod fm = new OVXFlowMod(
                this.factory.buildFlowModify()
                        .setMatch(match)
                        .setBufferId(this.getPacketIn().getBufferId())
                        .setHardTimeout(1)
                        .build()
        );

        sw.sendMsg(fm, sw);
    }

    private Integer fetchTenantId(final Match match, final Mappable map,
                                  final boolean useMAC) {
        MacAddress mac = match.get(MatchField.ETH_SRC);
        if (useMAC && map.hasMAC(mac)) {
            try {
                return map.getMAC(mac);
            } catch (AddressMappingException e) {
                log.warn("Tried to return non-mapped MAC address : {}", e);
            }
        }
        return null;
    }

    private OVXSwitch fetchOVXSwitch(PhysicalSwitch psw, OVXSwitch vswitch, Mappable map) {
        if (vswitch == null) {
            try {
                vswitch = map.getVirtualSwitch(psw, this.tenantId);
            } catch (SwitchMappingException e) {
                log.warn("Cannot fetch non-mapped OVXSwitch: {}", e);
            }
        }
        return vswitch;
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
