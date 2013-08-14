/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.packet.ARP;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.packet.IPv4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.Wildcards.Flag;

public class OVXPacketIn extends OFPacketIn implements Virtualizable {

    private Logger log = LogManager.getLogger(OVXPacketIn.class.getName());

    @Override
    public void virtualize(PhysicalSwitch sw) {

	Integer tenantId = null;
	OVXSwitch vSwitch = null;
	/*
	 * Fetching port from the physical switch
	 */
	short inport = this.getInPort();
	PhysicalPort port = sw.getPort(inport);

	OVXMap map = OVXMap.getInstance();

	OFMatch match = new OFMatch();
	match.loadFromPacket(this.getPacketData(), inport);


	/*
	 * Check whether this packet arrived on
	 * an edge port. 
	 * 
	 * if it did we do not need to rewrite anything, 
	 * but just find which controller this should be 
	 * send to.
	 */
	if (port.isEdge()) {
	    tenantId = fetchTenantId(true);
	    if (tenantId == null) {
		log.warn("PacketIn {} does not belong to any virtual network; "
			+ "dropping and intalling a temporary drop rule", this);
		installDropRule(sw, match);
		return;
	    }
	    vSwitch = map.getVirtualSwitch(sw, tenantId);
	    sendPkt(vSwitch, match, sw, tenantId);
	    learnAddresses(match, map, tenantId);
	    log.debug("Edge PacketIn {} sent to virtual network {}", this, tenantId);
	    return;
	} 

	/*
	 * Below handles packets traveling in the core.
	 */


	if (match.getDataLayerType() == Ethernet.TYPE_IPv4 || match.getDataLayerType() == Ethernet.TYPE_ARP) {
	    PhysicalIPAddress srcIP = new PhysicalIPAddress(match.getNetworkSource());
	    PhysicalIPAddress dstIP = new PhysicalIPAddress(match.getNetworkDestination());

	    Ethernet eth = new Ethernet();
	    eth.deserialize(this.getPacketData(), 0, this.getPacketData().length);
	    if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
		//ARP packet
		ARP arp = (ARP) eth.getPayload();
		//TODO: tenantId = map.getVirtualIP(srcIP).getTenantId();
		//TODO: arp.setSenderProtocolAddress(map.getVirtualIP(srcIP));
		//TODO: arp.setTargetProtocolAddress(map.getVirtualIP(dstIP));
	    } else if (match.getDataLayerType() == Ethernet.TYPE_IPv4) {
		IPv4 ip = (IPv4) eth.getPayload();
		//TODO: tenantId = map.getVirtualIP(srcIP).getTenantId();
		//TODO: ip.setDestinationAddress(map.getVirtualIP(dstIP));
		//TODO: ip.setSourceAddress(map.getVirtualIP(srcIP));
	    } else {
		log.info("{} handling not yet implemented; dropping", match.getDataLayerType());
		installDropRule(sw, match);
		return;
	    }
	    this.setPacketData(eth.serialize());
	    vSwitch = map.getVirtualSwitch(sw, tenantId);
	    sendPkt(vSwitch, match, sw, tenantId);
	    log.debug("IPv4 PacketIn {} sent to virtual network {}", this, tenantId);
	    return;
	}

	tenantId = fetchTenantId(true);
	if (tenantId == null) {
	    log.warn("PacketIn {} does not belong to any virtual network; "
			+ "dropping and intalling a temporary drop rule", this);
		installDropRule(sw, match);
		return;
	}
	vSwitch = map.getVirtualSwitch(sw, tenantId);
	sendPkt(vSwitch, match, sw, tenantId);
	log.debug("Layer2 PacketIn {} sent to virtual network {}", this, tenantId);
    }


    private void sendPkt(OVXSwitch vSwitch, OFMatch match, PhysicalSwitch sw, int tenantId) {
	if (vSwitch == null || !vSwitch.isActive()) {
	    log.warn("Controller for virtual network {} has not yet connected "
		    + "or is down", tenantId);
	    installDropRule(sw, match);
	    return;
	}
	vSwitch.sendMsg(this, sw);
    }

    private void learnAddresses(OFMatch match, OVXMap map, int tenantId) {
	if (match.getDataLayerType() == 0x800 || match.getDataLayerType() == 0x806) {
	    OVXNetwork vnet = map.getVirtualNetwork(tenantId);

	    if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) { //TODO check if we already know it) {
		PhysicalIPAddress pip = new PhysicalIPAddress(vnet.nextIP());
		OVXIPAddress vip = new OVXIPAddress(tenantId, match.getNetworkSource());
		map.addIP(pip, vip);
	    }
	    if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) { //TODO check if we already know it) {
		PhysicalIPAddress pip = new PhysicalIPAddress(vnet.nextIP());
		OVXIPAddress vip = new OVXIPAddress(tenantId, match.getNetworkDestination());
		map.addIP(pip, vip);
	    }
	}
    }

    private void installDropRule(PhysicalSwitch sw, OFMatch match) {
	OVXFlowMod fm = new OVXFlowMod();
	fm.setMatch(match);
	fm.setBufferId(this.getBufferId());
	fm.setHardTimeout((short) 1);
	sw.sendMsg(fm, sw);
    }

    private Integer fetchTenantId(boolean useMAC) {
	//TODO: fetch tenantId
	return null;
    }

}
