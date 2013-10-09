/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.datapath;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.LinkPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;

import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.util.LRULinkedHashMap;

/**
 * The Class OVXSwitch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> {

    /**
     * Datapath description string should this be made specific per type of
     * virtual switch
     */
    public static final String                       DPDESCSTRING     = "OpenVirteX Virtual Switch";

    /** The supported actions. */
    protected static int                             supportedActions = 0xFFF;

    /** The buffer dimension. */
    protected static int                             bufferDimension  = 4096;

    /** The tenant id. */
    protected Integer                                tenantId         = 0;

    /** The miss send len. */
    protected Short                                  missSendLen      = 0;

    /** The is active. */
    protected boolean                                isActive         = false;

    /** The capabilities. */
    protected OVXSwitchCapabilities                  capabilities;

    /** The backoff counter for this switch when unconnected */
    private AtomicInteger                            backOffCounter   = null;

    /**
     * The buffer map
     */
    protected LRULinkedHashMap<Integer, OVXPacketIn> bufferMap;

    private AtomicInteger                            bufferId         = null;

    private final AtomicInteger                      portCounter;

    /**
     * The virtual flow table
     */
    protected OVXFlowTable                           flowTable;

    /**
     * Instantiates a new OVX switch.
     * 
     * @param switchId
     *            the switch id
     * @param tenantId
     *            the tenant id
     */
    protected OVXSwitch(final Long switchId, final Integer tenantId) {
	super(switchId);
	this.tenantId = tenantId;
	this.missSendLen = 0;
	this.isActive = false;
	this.capabilities = new OVXSwitchCapabilities();
	this.backOffCounter = new AtomicInteger();
	this.resetBackOff();
	this.bufferMap = new LRULinkedHashMap<Integer, OVXPacketIn>(
	        OVXSwitch.bufferDimension);
	this.portCounter = new AtomicInteger(1);
	this.bufferId = new AtomicInteger(1);
	this.flowTable = new OVXFlowTable(this);
	// this.switchName = "OpenVirteX Virtual Switch 1.0";
    }

    /**
     * Gets the tenant id.
     * 
     * @return the tenant id
     */
    public Integer getTenantId() {
	return this.tenantId;
    }

    /**
     * Gets the miss send len.
     * 
     * @return the miss send len
     */
    public short getMissSendLen() {
	return this.missSendLen;
    }

    /**
     * Sets the miss send len.
     * 
     * @param missSendLen
     *            the miss send len
     * @return true, if successful
     */
    public boolean setMissSendLen(final Short missSendLen) {
	this.missSendLen = missSendLen;
	return true;
    }

    /**
     * Checks if is active.
     * 
     * @return true, if is active
     */
    public boolean isActive() {
	return this.isActive;
    }

    /**
     * Sets the active.
     * 
     * @param isActive
     *            the new active
     */
    public void setActive(final boolean isActive) {
	this.isActive = isActive;
    }

    /**
     * Gets the physical port number.
     * 
     * @param ovxPortNumber
     *            the ovx port number
     * @return the physical port number
     */
    public Short getPhysicalPortNumber(final Short ovxPortNumber) {
	return this.portMap.get(ovxPortNumber).getPhysicalPortNumber();
    }

    public void resetBackOff() {
	this.backOffCounter.set(-1);
    }

    public int incrementBackOff() {
	return this.backOffCounter.incrementAndGet();
    }

    // /**
    // * Gets the new port number.
    // *
    // * @return the new port number
    // */
    // private Short getNewPortNumber() {
    // short portNumber = 1;
    // final Set<Short> keys = this.portMap.keySet();
    //
    // if (keys.isEmpty()) {
    // return portNumber;
    // } else {
    // boolean solved = false;
    // while (solved == false && portNumber < 256) {
    // if (!keys.contains(portNumber)) {
    // solved = true;
    // } else {
    // portNumber += 1;
    // }
    // }
    // if (solved == true) {
    // return portNumber;
    // } else {
    // return 0;
    // }
    // }
    // }

    // TODO: add check for maximum value
    // TODO: use bitmap to keep track of released port numbers
    public short getNextPortNumber() {
	return (short) this.portCounter.getAndIncrement();
    }

    protected void addDefaultPort(final LinkedList<OFPhysicalPort> ports) {
	final OFPhysicalPort port = new OFPhysicalPort();
	port.setPortNumber(OFPort.OFPP_LOCAL.getValue());
	port.setName("OpenFlow Local Port");
	port.setConfig(1);
	final byte[] addr = { (byte) 0xA4, (byte) 0x23, (byte) 0x05,
	        (byte) 0x00, (byte) 0x00, (byte) 0x00 };
	port.setHardwareAddress(addr);
	port.setState(1);
	port.setAdvertisedFeatures(0);
	port.setCurrentFeatures(0);
	port.setSupportedFeatures(0);
	ports.add(port);
    }

    public void register(final List<PhysicalSwitch> physicalSwitches) {
	this.map.addSwitches(physicalSwitches, this);
    }

    public void unregister() {
	this.isActive = false;
	if (this.getPorts() != null) {
	    final Set<Short> portSet = new TreeSet<Short>(this.getPorts()
		    .keySet());
	    for (final Short portNumber : portSet) {
		// TODO: after merging with Ayaka, retrieve link from port and
		// not from linkId
		final OVXPort port = this.getPort(portNumber);
		if (port.isEdge()) {
		    port.unregister();
		} else {
		    LinkPair<OVXLink> links = port.getLink();
		    if ((links != null) && (links.exists())) {
			links.getOutLink().unregister();
			links.getInLink().unregister();
		    }
		    /*final OVXNetwork virtualNetwork = this.map
			    .getVirtualNetwork(this.tenantId);
		    final OVXPort neighPort = virtualNetwork
			    .getNeighborPort(port);
		    virtualNetwork.getLink(port, neighPort).unregister();
		    virtualNetwork.getLink(neighPort, port).unregister(); */
		}
	    }
	}

	// remove the switch from the map
	this.map.getVirtualNetwork(this.tenantId).removeSwitch(this);
	this.map.removeVirtualSwitch(this);
	this.tearDown();
    }

    @Override
    public void tearDown() {
	this.isActive = false;
	if (this.channel != null)
	    this.channel.close();
    }

    /**
     * Generate features reply.
     */
    public void generateFeaturesReply() {
	final OFFeaturesReply ofReply = new OFFeaturesReply();
	ofReply.setDatapathId(this.switchId);
	final LinkedList<OFPhysicalPort> portList = new LinkedList<OFPhysicalPort>();
	for (final OVXPort ovxPort : this.portMap.values()) {
	    final OFPhysicalPort ofPort = new OFPhysicalPort();
	    ofPort.setPortNumber(ovxPort.getPortNumber());
	    ofPort.setName(ovxPort.getName());
	    ofPort.setConfig(ovxPort.getConfig());
	    ofPort.setHardwareAddress(ovxPort.getHardwareAddress());
	    ofPort.setState(ovxPort.getState());
	    ofPort.setAdvertisedFeatures(ovxPort.getAdvertisedFeatures());
	    ofPort.setCurrentFeatures(ovxPort.getCurrentFeatures());
	    ofPort.setSupportedFeatures(ovxPort.getSupportedFeatures());
	    portList.add(ofPort);
	}

	/*
	 * Giving the switch a port (the local port) which is set
	 * administratively down.
	 * 
	 * Perhaps this can be used to send the packets to somewhere
	 * interesting.
	 */
	this.addDefaultPort(portList);
	ofReply.setPorts(portList);
	ofReply.setBuffers(OVXSwitch.bufferDimension);
	ofReply.setTables((byte) 1);
	ofReply.setCapabilities(this.capabilities.getOVXSwitchCapabilities());
	ofReply.setActions(OVXSwitch.supportedActions);
	ofReply.setXid(0);
	ofReply.setLengthU(OFFeaturesReply.MINIMUM_LENGTH
	        + OFPhysicalPort.MINIMUM_LENGTH * portList.size());

	this.setFeaturesReply(ofReply);
    }

    /**
     * Boots virtual switch by connecting it to the controller TODO: should
     * 
     * @return True if successful, false otherwise
     */
    @Override
    public boolean boot() {
	this.generateFeaturesReply();
	final OpenVirteXController ovxController = OpenVirteXController
	        .getInstance();
	ovxController.registerOVXSwitch(this);
	this.setActive(true);
	return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
     */
    @Override
    public String toString() {
	return "SWITCH: switchId: " + this.switchId + " - switchName: "
	        + this.switchName + " - isConnected: " + this.isConnected
	        + " - tenantId: " + this.tenantId + " - missSendLength: "
	        + this.missSendLen + " - isActive: " + this.isActive
	        + " - capabilities: "
	        + this.capabilities.getOVXSwitchCapabilities();
    }

    public synchronized int addToBufferMap(final OVXPacketIn pktIn) {
	// TODO: this isn't thread safe... fix it
	this.bufferId.compareAndSet(OVXSwitch.bufferDimension, 0);
	this.bufferMap.put(this.bufferId.get(), new OVXPacketIn(pktIn));
	return this.bufferId.getAndIncrement();
    }

    public OVXPacketIn getFromBufferMap(final Integer bufId) {
	return this.bufferMap.get(bufId);
    }

    @Override
    public boolean equals(final Object other) {
	// TODO: fix this big shit
	if (other instanceof OVXSwitch) {
	    final OVXSwitch that = (OVXSwitch) other;
	    return this.switchId == that.switchId
		    && this.tenantId == that.tenantId;
	}
	return false;
    }

    public OVXFlowTable getFlowTable() {
	return this.flowTable;
    }

    /**
     * get a OVXFlowMod out of the map
     * 
     * @param cookie
     *            the physical cookie
     * @return
     */
    public OVXFlowMod getFlowMod(final Long cookie) {
	return this.flowTable.getFlowMod(cookie);
    }

    /**
     * Add a FlowMod to the mapping
     * 
     * @param flowmod
     * @return the new physical cookie
     */

    public long addFlowMod(final OVXFlowMod flowmod) {
	return this.flowTable.addFlowMod(flowmod);
    }

    /**
     * Remove an entry in the mapping
     * 
     * @param cookie
     * @return The deleted FlowMod
     */
    public OVXFlowMod deleteFlowMod(final Long cookie) {
	return this.flowTable.deleteFlowMod(cookie);
    }

    /**
     * Generates a new XID for messages destined for the physical network.
     * 
     * @param msg The OFMessage being translated
     * @param inPort The ingress port 
     * @return the new message XID
     */
    public abstract int translate(OFMessage msg, OVXPort inPort);

    /**
     * Sends a message towards the physical network
     * 
     * @param msg The OFMessage being translated
     * @param inPort The ingress port
     */
    public abstract void sendSouth(OFMessage msg, OVXPort inPort);
}
