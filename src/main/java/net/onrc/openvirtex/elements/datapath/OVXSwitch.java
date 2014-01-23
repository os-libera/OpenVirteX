/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Persistable;

import java.util.Set;
import java.util.TreeSet;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.util.LRULinkedHashMap;

/**
 * The Class OVXSwitch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> implements Persistable {

	private static Logger log = LogManager.getLogger(OVXSwitch.class
			.getName());

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

	/** The miss send len. Default in spec is 128 */
	protected Short                                  missSendLen      = 128; 

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

	private final BitSetIndex                      portCounter;

	/**
	 * The virtual flow table
	 */
	protected FlowTable                           flowTable;

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
		this.portCounter = new BitSetIndex(IndexType.PORT_ID);
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

	public short getNextPortNumber() throws IndexOutOfBoundException {
		return this.portCounter.getNewIndex().shortValue();
	}

	public void relesePortNumber(short portNumber) {
		this.portCounter.releaseIndex((int) portNumber);
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
		DBManager.getInstance().save(this);
	}

	public void unregister() {
		DBManager.getInstance().remove(this);
		this.isActive = false;
		if (this.getPorts() != null) {
			OVXNetwork net;
			try {
				net = this.getMap().getVirtualNetwork(this.tenantId);
			} catch (NetworkMappingException e) {
				log.error("Error retrieving the network with id {}. Unregister for OVXSwitch {} not fully done!", this.getTenantId(), this.getSwitchName());
				return;
			}
			final Set<Short> portSet = new TreeSet<Short>(this.getPorts()
					.keySet());
			for (final Short portNumber : portSet) {
				final OVXPort port = this.getPort(portNumber);
				if (port.isEdge()) {
					Host h = net.getHost(port);
					if (h != null) 
						net.getHostCounter().releaseIndex(h.getHostId());
				} else {
					net.getLinkCounter().releaseIndex(port.getLink().getInLink().getLinkId());
				}
				port.unregister();
			}
		}
		// remove the switch from the map
		try {
			this.map.getVirtualNetwork(this.tenantId).removeSwitch(this);
		} catch (NetworkMappingException e) {
			log.warn(e.getMessage());
		}

		cleanUpFlowMods(false);

		this.map.removeVirtualSwitch(this);
		this.tearDown();
	}

	private void cleanUpFlowMods(boolean isOk) {
		log.info("Cleaning up flowmods");
		List<PhysicalSwitch> physicalSwitches;
		try {
			physicalSwitches = this.map.getPhysicalSwitches(this);
		} catch (SwitchMappingException e) {
			if (!isOk)
				log.warn("Failed to cleanUp flowmods for tenant {} on switch {}", this.tenantId, this.getSwitchName());
			return;
		}
		for (PhysicalSwitch sw : physicalSwitches) 
			sw.cleanUpTenant(this.tenantId, (short) 0);

	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.tenantId);
		return index;
	}

	@Override
	public String getDBKey() {
		return Switch.DB_KEY;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.put(TenantHandler.VDPID, this.switchId);
		List<Long> switches = new ArrayList<Long>();
		try {
			for (PhysicalSwitch sw: this.map.getPhysicalSwitches(this)) {
				switches.add(sw.getSwitchId());
			}
		} catch (SwitchMappingException e) {
			return null;
		}
		dbObject.put(TenantHandler.DPIDS, switches);
		return dbObject;
	}	

	@Override
	public void tearDown() {
		this.isActive = false;
		if (this.channel != null)
			this.channel.close();

		cleanUpFlowMods(true);
		for (OVXPort p : getPorts().values()) {
			if (p.isLink()) 
				p.tearDown();
		}
	
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
		for (OVXPort p : getPorts().values()) {
			if (p.isLink()) {
				p.boot();
			}	
		}
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

	

	public FlowTable getFlowTable() {
		return this.flowTable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tenantId == null) ? 0 : tenantId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof OVXSwitch))
			return false;
		OVXSwitch other = (OVXSwitch) obj;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} 
		return this.switchId == other.switchId
				&& this.tenantId == other.tenantId;
	}

	/**
	 * get a OVXFlowMod out of the map
	 * 
	 * @param cookie
	 *            the physical cookie
	 * @return
	 * @throws MappingException 
	 */
	public OVXFlowMod getFlowMod(final Long cookie) throws MappingException {
		return this.flowTable.getFlowMod(cookie);
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
	 * @throws SwitchMappingException 
	 */
	public abstract int translate(OFMessage msg, OVXPort inPort);

	/**
	 * Sends a message towards the physical network, via the PhysicalSwitch mapped to this OVXSwitch. 
	 * 
	 * @param msg The OFMessage being translated
	 * @param inPort The ingress port, used to identify the PhysicalSwitch underlying an OVXBigSwitch. May be null. 
	 * Sends a message towards the physical network
	 * 
	 * @param msg The OFMessage being translated
	 * @param inPort The ingress port
	 */
	public abstract void sendSouth(OFMessage msg, OVXPort inPort);

}
