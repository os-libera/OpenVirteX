/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 * Virtual networks contain tenantId, controller info, subnet and gateway
 * information. Handles registration of virtual switches and links. Responds to
 * LLDP discovery probes from the controller.
 * 
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {
    static Logger                                log             = LogManager
	                                                                 .getLogger(OVXNetwork.class
	                                                                         .getName());

    private final Integer                        tenantId;
    private final String                         protocol;
    private final String                         controllerHost;
    private final Integer                        controllerPort;
    private final IPAddress                      network;
    private final short                          mask;
    private HashMap<IPAddress, MACAddress>       gwsMap;
    private boolean                              bootState;
    private static BitSetIndex                   tenantIdCounter = new BitSetIndex(
	                                                                 IndexType.TENANT_ID);
    private final BitSetIndex                    dpidCounter;
    private final BitSetIndex                    linkCounter;
    private final BitSetIndex                    ipCounter;
    private final List<Host>                     hostList;
    private final HashBiMap<Integer, BigInteger> flowValues;
    private final BitSetIndex                    flowCounter;

    public OVXNetwork(final String protocol, final String controllerHost,
	    final Integer controllerPort, final IPAddress network,
	    final short mask) throws IndexOutOfBoundException {
	super();
	this.tenantId = OVXNetwork.tenantIdCounter.getNewIndex();
	this.protocol = protocol;
	this.controllerHost = controllerHost;
	this.controllerPort = controllerPort;
	this.network = network;
	this.mask = mask;
	this.bootState = false;
	this.dpidCounter = new BitSetIndex(IndexType.SWITCH_ID);
	this.linkCounter = new BitSetIndex(IndexType.LINK_ID);
	this.ipCounter = new BitSetIndex(IndexType.IP_ID);
	this.hostList = new LinkedList<Host>();
	this.flowValues = HashBiMap.create();
	this.flowCounter = new BitSetIndex(IndexType.FLOW_COUNTER);
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

    public boolean isBooted() {
	return this.bootState;
    }

    public List<Host> getHosts() {
	return Collections.unmodifiableList(this.hostList);
    }

    public Host getHost(final MACAddress mac) {
	for (final Host host : this.hostList) {
	    if (host.getMac().toLong() == mac.toLong()) {
		return host;
	    }
	}
	return null;
    }

    public Host getHost(final OVXPort port) {
	for (final Host host : this.hostList) {
	    if (host.getPort().equals(port)) {
		return host;
	    }
	}
	return null;
    }

    /**
     * Get list of all registered MAC addresses in this virtual network
     */
    private List<MACAddress> getMACList() {
	final List<MACAddress> result = new LinkedList<MACAddress>();
	for (final Host host : this.hostList) {
	    result.add(host.getMac());
	}
	return result;
    }

    public void unregister() {
	final LinkedList<Long> dpids = new LinkedList<>();
	for (final OVXSwitch virtualSwitch : this.getSwitches()) {
	    dpids.add(virtualSwitch.getSwitchId());
	}
	for (final Long dpid : dpids) {
	    this.getSwitch(dpid).unregister();
	}
	// remove the network from the Map
	OVXMap.getInstance().removeVirtualIPs(this.tenantId);
	OVXMap.getInstance().removeNetwork(this);
	OVXNetwork.tenantIdCounter.releaseIndex(this.tenantId);
    }

    public void stop() {
	for (final OVXSwitch sw : this.getSwitches()) {
	    sw.tearDown();
	}
	this.bootState = false;
    }

    // API-facing methods

    public OVXSwitch createSwitch(final List<Long> dpids)
	    throws IndexOutOfBoundException {
	OVXSwitch virtualSwitch;
	/*
	 * The switchId is generated using the ON.Lab OUI (00:A4:23:05)
	 * plus a unique number inside the virtual network
	 */
	final long switchId = (long) 0xa42305 << 32
	        | this.dpidCounter.getNewIndex();
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
	if (this.bootState) {
	    virtualSwitch.boot();
	}

	return virtualSwitch;
    }

    /**
     * Create link and add it to the topology. Returns linkId when successful,
     * -1 if source port is already used.
     * 
     * @param srcPort
     * @param dstPort
     * @return
     * @throws IndexOutOfBoundException
     */
    public synchronized OVXLink createLink(
	    final List<PhysicalLink> physicalLinks)
	    throws IndexOutOfBoundException {
	// Create and register virtual source and destination ports
	final PhysicalPort phySrcPort = physicalLinks.get(0).getSrcPort();
	final OVXPort srcPort = new OVXPort(this.tenantId, phySrcPort, false);
	final PhysicalPort phyDstPort = physicalLinks.get(
	        physicalLinks.size() - 1).getDstPort();
	final OVXPort dstPort = new OVXPort(this.tenantId, phyDstPort, false);

	// Create link, add it to the topology, register it in the map
	final int linkId = this.linkCounter.getNewIndex();
	// Set the linkId value inside the src and dst virtual ports
	srcPort.setLinkId(linkId);
	dstPort.setLinkId(linkId);
	srcPort.register();
	dstPort.register();

	final OVXLink link = new OVXLink(linkId, this.tenantId, srcPort,
	        dstPort);
	final OVXLink reverseLink = new OVXLink(linkId, this.tenantId, dstPort,
	        srcPort);
	super.addLink(link);
	super.addLink(reverseLink);
	link.register(physicalLinks);
	// create the reverse list of physical links
	final List<PhysicalLink> reversePhysicalLinks = new LinkedList<PhysicalLink>();
	for (final PhysicalLink phyLink : Lists.reverse(physicalLinks)) {
	    reversePhysicalLinks.add(new PhysicalLink(phyLink.getDstPort(),
		    phyLink.getSrcPort()));

	}
	reverseLink.register(reversePhysicalLinks);
	return link;
    }

    public OVXPort createHost(final long physicalDpid, final short portNumber,
	    final MACAddress mac) throws IndexOutOfBoundException {
	// TODO: check if dpid & port exist
	final PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance()
	        .getSwitch(physicalDpid);
	final PhysicalPort physicalPort = physicalSwitch.getPort(portNumber);

	final OVXPort edgePort = new OVXPort(this.tenantId, physicalPort, true);
	edgePort.register();
	OVXMap.getInstance().addMAC(mac, this.tenantId);
	final Host host = new Host(mac, edgePort);
	this.hostList.add(host);
	return edgePort;

    }

    /**
     * Boots the virtual network by booting each virtual switch. TODO: we should
     * roll-back if any switch fails to boot
     * 
     * @return True if successful, false otherwise
     */
    @Override
    public boolean boot() {
	boolean result = true;
	try {
	    this.generateFlowPairs();
	} catch (final IndexOutOfBoundException e) {
	    OVXNetwork.log
		    .error("Too many host to generate the flow pairs. Tear down the virtual network {}",
		            this.tenantId);
	    this.stop();
	    return false;
	}
	for (final OVXSwitch sw : this.getSwitches()) {
	    result &= sw.boot();
	}
	this.bootState = result;
	return this.bootState;
    }

    public Integer storeFlowValues(final byte[] srcMac, final byte[] dstMac)
	    throws IndexOutOfBoundException {
	// TODO: Optimize flow numbers
	final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
	        dstMac));
	Integer flowId = this.flowValues.inverse().get(dualMac);
	if (flowId == null) {
	    flowId = this.flowCounter.getNewIndex();
	    OVXNetwork.log
		    .debug("virtual net = {}: save flowId = {} that is associated to {} {}",
		            this.tenantId, flowId, MACAddress.valueOf(srcMac)
		                    .toString(), MACAddress.valueOf(dstMac)
		                    .toString());
	    this.flowValues.put(flowId, dualMac);
	}
	return flowId;
    }

    public LinkedList<MACAddress> getFlowValues(final Integer flowId) {
	final LinkedList<MACAddress> macList = new LinkedList<MACAddress>();
	final BigInteger dualMac = this.flowValues.get(flowId);
	if (dualMac != null) {
	    final MACAddress srcMac = MACAddress.valueOf(dualMac.shiftRight(48)
		    .longValue());
	    final MACAddress dstMac = MACAddress.valueOf(dualMac.longValue());
	    macList.add(srcMac);
	    macList.add(dstMac);
	}
	return macList;
    }

    public Integer getFlowId(final byte[] srcMac, final byte[] dstMac) {
	if (MACAddress.valueOf(srcMac).toLong() == 0 || MACAddress.valueOf(srcMac).toLong() == 0) {
	    log.warn("virtual net = {}: OVX doens't store flowId associated to mac address == 00:00:00:00:00:00", this.tenantId);
	    return 0;
	}
	else {
	    final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
		    dstMac));
	    OVXNetwork.log
	    .debug("virtual net = {}: retrieving flowId that is associated to {} {}",
		    this.tenantId, MACAddress.valueOf(srcMac).toString(),
		    MACAddress.valueOf(dstMac).toString());
	    final Integer flowId = this.flowValues.inverse().get(dualMac);
	    if (flowId == 0) {
		throw new VirtualLinkException("");
	    }
	    return flowId;
	}
    }

    private void generateFlowPairs() throws IndexOutOfBoundException {
	final List<MACAddress> macList = this.getMACList();
	for (final MACAddress srcMac : macList) {
	    this.storeFlowValues(srcMac.toBytes(), MACAddress.valueOf("ff:ff:ff:ff:ff:ff").toBytes());
	    for (final MACAddress dstMac : macList) {
		if (srcMac.toLong() != dstMac.toLong()) {
		    this.storeFlowValues(srcMac.toBytes(), dstMac.toBytes());

		}
	    }
	}
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
			pi.setPacketData(pkt);
			pi.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + pkt.length));
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

    public Integer nextIP() throws IndexOutOfBoundException {
	return (this.tenantId << 32 - OpenVirteXController.getInstance()
	        .getNumberVirtualNets()) + this.ipCounter.getNewIndex();
    }

    public static void reset() {
	OVXNetwork.log
	        .debug("Resetting tenantId counter to initial state. Don't do this at runtime!");
	OVXNetwork.tenantIdCounter.reset();

    }

    public LinkedList<OVXLink> getLinksById(final int linkId) {
	final LinkedList<OVXLink> linkList = new LinkedList<OVXLink>();
	for (final OVXLink link : this.linkSet) {
	    if (link.getLinkId() == linkId) {
		linkList.add(link);
	    }
	}
	if (linkList.size() == 2) {
	    return linkList;
	}
	return null;
    }

    public Set<OVXLink> getLinkSet() {
	return Collections.unmodifiableSet(this.linkSet);
    }

    @Override
    public boolean removeLink(final OVXLink virtualLink) {
	return this.linkSet.remove(virtualLink);
    }

    @Override
    public boolean removeSwitch(final OVXSwitch ovxSwitch) {
	return this.switchSet.remove(ovxSwitch);
    }

    public boolean removeHost(final MACAddress hostAddress) {
	final Host host = this.getHost(hostAddress);
	return this.hostList.remove(host);
    }
}
