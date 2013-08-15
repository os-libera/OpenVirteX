/**
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package net.onrc.openvirtex.elements.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import java.util.HashSet;
import java.util.LinkedList;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Virtual networks contain tenantId, controller info, subnet and gateway
 * information. Handles registration of virtual switches and links. Responds to
 * LLDP discovery probes from the controller.
 * 
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {


    private final Integer                  tenantId;
    private final String                   protocol;
    private final String                   controllerHost;
    private final Integer                  controllerPort;
    private final IPAddress                network;
    private final short                    mask;
    private HashMap<IPAddress, MACAddress> gwsMap;
    private boolean                        bootState;
    private static AtomicInteger           tenantIdCounter = new AtomicInteger(
	                                                           1);
    private final AtomicLong               dpidCounter;
    private final AtomicInteger            linkCounter;
    private final AtomicInteger		   ipCounter;
    
    // TODO: implement vlink flow pusher
    // public VLinkManager vLinkMgmt;

    Logger log = LogManager.getLogger(OVXNetwork.class.getName());


    public OVXNetwork(final String protocol, final String controllerHost,
	    final Integer controllerPort, final IPAddress network,
	    final short mask) {
	super();
	this.tenantId = OVXNetwork.tenantIdCounter.getAndIncrement();
	this.protocol = protocol;
	this.controllerHost = controllerHost;
	this.controllerPort = controllerPort;
	this.network = network;
	this.mask = mask;
	this.bootState = false;
	this.dpidCounter = new AtomicLong(1);
	// TODO: decide which value to start linkId's
	this.linkCounter = new AtomicInteger(2);
	this.ipCounter = new AtomicInteger();
    }

    public String getProtocol() {
	return this.protocol;
    }

    public String getControllerHost() {
	return this.controllerHost;
    }

    public Integer getControllerPort() {
	return this.controllerPort;
    }

    public Integer getTenantId() {
	return this.tenantId;
    }

    public IPAddress getNetwork() {
	return this.network;
    }

    public MACAddress getGateway(final IPAddress ip) {
	return this.gwsMap.get(ip);
    }

    public short getMask() {
	return this.mask;
    }

    public void register() {
	OVXMap.getInstance().addNetwork(this);
    }

    // API-facing methods

    public OVXSwitch createSwitch(final List<Long> dpids) {
	OVXSwitch virtualSwitch;
	// TODO: generate ON.Lab dpid's
	final long switchId = this.dpidCounter.getAndIncrement();
	final List<PhysicalSwitch> switches = new ArrayList<PhysicalSwitch>();
	// TODO: check if dpids are present in physical network
	for (final long dpid : dpids) {
	    switches.add(PhysicalNetwork.getInstance().getSwitch(dpid));
	}

	if (dpids.size() == 1) {
	    virtualSwitch = new OVXSingleSwitch(switchId, this.tenantId);
	} else {
	    virtualSwitch = new OVXBigSwitch(switchId, this.tenantId);
	}
	// Add switch to topology and register it in the map
	this.addSwitch(virtualSwitch);
	virtualSwitch.register(switches);
	return virtualSwitch;
    }

	public HashMap<IPAddress, MACAddress> getGwsMap() {
		return this.gwsMap;
	}



    /**
     * Create link and add it to the topology. Returns linkId when successful,
     * -1 if source port is already used.
     * 
     * @param srcPort
     * @param dstPort
     * @return
     */
    public synchronized OVXLink createLink(
	    final List<PhysicalLink> physicalLinks) {
	// Create and register virtual source and destination ports
	final PhysicalPort phySrcPort = physicalLinks.get(0).getSrcPort();
	final OVXPort srcPort = new OVXPort(this.tenantId, phySrcPort, false);
	srcPort.register();
	final PhysicalPort phyDstPort = physicalLinks.get(
	        physicalLinks.size() - 1).getDstPort();
	final OVXPort dstPort = new OVXPort(this.tenantId, phyDstPort, false);
	dstPort.register();
	// Create link, add it to the topology, register it in the map
	final int linkId = this.linkCounter.getAndIncrement();
	final OVXLink link = new OVXLink(linkId, this.tenantId, srcPort,
	        dstPort);
	final OVXLink reverseLink = new OVXLink(linkId, this.tenantId, dstPort,
	        srcPort);
	super.addLink(link);
	super.addLink(reverseLink);
	link.register(physicalLinks);
	reverseLink.register(physicalLinks);

	return link;
    }

    public OVXPort createHost(final long physicalDpid, final short portNumber,
	    final MACAddress mac) {
	// TODO: check if dpid & port exist
	final PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance()
	        .getSwitch(physicalDpid);
	final PhysicalPort physicalPort = physicalSwitch.getPort(portNumber);

	final OVXPort edgePort = new OVXPort(this.tenantId, physicalPort, true);
	edgePort.register();
	OVXMap.getInstance().addMAC(mac, this.tenantId);

	return edgePort;
    }

    // TODO
    public void createGateway(final IPAddress ip) {

    }


    /**
     * Boots the virtual network by booting each virtual switch.
     * TODO: we should roll-back if any switch fails to boot
     * 
     * @return
     *         True if successful, false otherwise
     */
    @Override
    public boolean boot() {
	boolean result = true;
	for (final OVXSwitch sw : this.getSwitches()) {
	    result &= sw.boot();
	}
	this.bootState = result;
	return this.bootState;
    }

    /**
     * Handle LLDP received from controller.
     * 
     * Receive LLDP from controller. Switch to which it is destined is passed in
     * by the ControllerHandler, port is extracted from the packet_out.
     * Packet_in is created based on topology info.
     */
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final OVXPacketOut po = (OVXPacketOut) msg;
	final byte[] pkt = po.getPacketData();
	if (LLDPUtil.checkLLDP(pkt)) {
	    // Create LLDP response for each output action port
	    for (final OFAction action : po.getActions()) {
		try {
		    final short portNumber = ((OFActionOutput) action)
			    .getPort();
		    final OVXPort srcPort = (OVXPort) sw.getPort(portNumber);
		    final OVXPort dstPort = this.getNeighborPort(srcPort);
		    if (dstPort != null) {
			final OVXPacketIn pi = new OVXPacketIn();
			pi.setBufferId(OFPacketOut.BUFFER_ID_NONE);
			// Get input port from pkt_out
			pi.setInPort(dstPort.getPortNumber());
			pi.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
			pi.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + pkt.length));
			pi.setPacketData(pkt);
			dstPort.getParentSwitch().sendMsg(pi, this);
		    }
		} catch (final ClassCastException c) {
		    // ignore non-ActionOutput pkt_out's
		}
	    }
	} else {
	    this.log.debug("Invalid LLDP");
	}

    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	// Do nothing
    }

    @Override
    public String getName() {
	return "Virtual network:" + this.tenantId.toString();
    }

    public Integer nextIP() {
	System.err.println(OpenVirteXController.getInstance().getNumberVirtualNets());
	return (this.tenantId<< 
		(32-OpenVirteXController.getInstance().getNumberVirtualNets())) 
			+ ipCounter.getAndIncrement();
    }

	/*public void fromJson(HashMap<String, Object> map) {
		    this.tenantId = (Integer) map.get("tenant-id");
		    this.network = (IPAddress) map.get("network");
		    /*for(Object ip : ((HashMap<Object,Object>) map.get("gateway")).keySet()){
			ip = (IPAddress) ip;
		    }
		    for(Object mac : ((HashMap<Object,Object>) map.get("gateway")).values()){
			mac = (MACAddress) mac;
		    }*/
		    /*this.gwsMap  = (HashMap<IPAddress,MACAddress>) map.get("gateway");
		    //this.gwsMap.put(ip, mac);
		    
        }*/
	
	    public HashMap<String,Object> toJson() {
		//HashMap<String,Object> output = new HashMap<String,Object>();
		//LinkedList<Object> list = new LinkedList<Object>();
		HashMap<String,Object> ovxMap = new HashMap<String,Object>();
		ovxMap.put("tenant-id",this.tenantId);
		String subnet = getNetworkWithMask(network,mask);
		ovxMap.put("network",subnet);
		ovxMap.put("gateway",this.gwsMap);
		LinkedList<String> switches = getDpids();//new LinkedList<Long>();
		ovxMap.put("switch-id",switches);
		ovxMap.put("controller-address", this.controllerHost);
		ovxMap.put("controller-port", this.controllerPort);
		//list.add(ovxMap);
		//output.put("virtualnetwork", list);
		return ovxMap; 
	    }

	    private LinkedList<String> getDpids() {
		Collection<OVXSwitch> switches = getSwitches();
		LinkedList<String> dpids = new LinkedList<String>();
		for(OVXSwitch sw: switches){
		    dpids.add(String.valueOf(sw.getSwitchId()));
		}
	        return dpids;
            }
	    private String getNetworkWithMask(IPAddress network, short mask) {
	        String subnet = new String();
	        subnet.concat(network.toString());
	        subnet.concat("/");
	        subnet.concat(String.valueOf(mask));
	        return subnet;
            }
}
