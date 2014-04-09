/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;

import java.util.Set;

import net.onrc.openvirtex.elements.datapath.role.RoleManager;
import net.onrc.openvirtex.elements.datapath.role.RoleManager.Role;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ControllerStateException;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.UnknownRoleException;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.util.LRULinkedHashMap;
import org.openflow.vendor.nicira.OFNiciraVendorData;
import org.openflow.vendor.nicira.OFRoleReplyVendorData;
import org.openflow.vendor.nicira.OFRoleRequestVendorData;

/**
 * The Class OVXSwitch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> implements Persistable {

	/**
	 * Shared state machine between OVXSwitch subclasses. This includes:
	 * <ul>
	 *   <li>{@link OVXSingleSwitch}</li> 
	 *   <li>{@link OVXBigSwitch}</li> 
	 * </ul>
	 */
	enum SwitchState {
		INIT {
			public void register(OVXSwitch vsw, List<PhysicalSwitch> physicalSwitches) {
				log.debug("Registering switch {}", vsw.getSwitchName());
				OVXMap.getInstance().addSwitches(physicalSwitches, vsw);
				DBManager.getInstance().save(vsw);
				vsw.state = SwitchState.INACTIVE;
			}
		},
		INACTIVE {
			public boolean boot(OVXSwitch vsw) {
				log.debug("Booting switch {}", vsw.getSwitchName());
				return vsw.bootSwitch();
			}
			
			public void unregister(OVXSwitch vsw) {
				log.debug("Unregistering switch {}", vsw.getSwitchName());
				vsw.state = SwitchState.STOPPED;
				vsw.unregSwitch(false);
			}
		},
		ACTIVE {
			public boolean teardown(OVXSwitch vsw, boolean synch) {
				log.debug("Tearing down switch {}", vsw.getSwitchName());
				vsw.state = SwitchState.INACTIVE;
				vsw.roleMan.shutDown();
				if ((vsw.channel != null) && (vsw.channel.isOpen()))
					vsw.channel.close();
				/* if (synch), a PhysicalSwitch has already done this */
				if (!synch) {
					vsw.cleanUpFlowMods(true);
				}
				for (OVXPort p : vsw.getPorts().values()) {
					if (p.isLink()) 
						p.tearDown();
				}
				return true;
			}
			
			public void sendMsg(OVXSwitch vsw, final OFMessage msg, final OVXSendMsg from) {
				XidPair<Channel> pair = vsw.channelMux.untranslate(msg.getXid());
				Channel c = null;
				if (pair != null) {
					msg.setXid(pair.getXid());
					c = pair.getSwitch();
				} 

				if (vsw.isConnected) {
					vsw.roleMan.sendMsg(msg, c);
				} else {
					// TODO: we probably should install a drop rule here.
					log.warn("Virtual switch {} is not connected to a controller", vsw.switchName);
				}
			}
			
			public void handleIO(OVXSwitch vsw, final OFMessage msg, Channel ch) {
				/*
				 * Save the channel the msg came in on
				 */
				msg.setXid(vsw.channelMux.translate(msg.getXid(), ch));
				try {
					/*
					 * Check whether this channel (ie. controller) is permitted 
					 * to send this msg to the dataplane
					 */
					
					if (vsw.roleMan.canSend(ch, msg) )
						((Devirtualizable) msg).devirtualize(vsw);
					else
						vsw.denyAccess(ch, msg, vsw.roleMan.getRole(ch));
				} catch (final ClassCastException e) {
					OVXSwitch.log.error("Received illegal message : " + msg);
				}
			}
			
			public void handleRoleIO(OVXSwitch vsw, OFVendor msg, Channel ch) {
				Role role = vsw.extractNiciraRoleRequest(ch, msg);
				try {
					vsw.roleMan.setRole(ch, role);
					vsw.sendRoleReply(role, msg.getXid(), ch);
					log.info("Finished handling role for {}", ch.getRemoteAddress() );
				} catch (IllegalArgumentException | UnknownRoleException ex) {
					log.warn(ex.getMessage());
				} 
			}
		},
		STOPPED;
		
		public void register(OVXSwitch vsw, List<PhysicalSwitch> physicalSwitches) {
			log.debug("Switch {} has already been registered", vsw.getSwitchName());
		}
		
		public boolean boot(OVXSwitch vsw) {
			log.debug("Switch {} has already been enabled", vsw.getSwitchName());
			return false;
		}
		
		/**
		 * @param vsw
		 * @param synch true if teardown is initiated by a PhysicalSwitch.
		 * @return
		 */
		public boolean teardown(OVXSwitch vsw, boolean synch) {
			log.debug("Switch {} has already been disabled", vsw.getSwitchName());
			return false;
		}
		
		public void unregister(OVXSwitch vsw) {
			log.debug("Switch {} can't shut down from state={}"
					, vsw.state);
		}
		
		/** When ACTIVE, sends a message */
		public void sendMsg(OVXSwitch vsw, final OFMessage msg, final OVXSendMsg from) {
			log.debug("Tried sending message while Switch {} not ACTIVE", 
					vsw.getSwitchName());
		}
		
		/** 
		 * When ACTIVE, handles the de-virtualization of messages. Some other calls
		 * are 'locked' by this one being 'locked'. This includes:
		 * 
		 *   translate(),
		 *   addToBufferMap(),
		 *   deleteFlowMod(),
		 *   sendSouth()
		 */
		public void handleIO(OVXSwitch vsw, OFMessage msg, Channel ch) {
			log.debug("Tried to handle message \n{}\nwhile Switch {} not ACTIVE", 
					msg, vsw.getSwitchName());
		}

		public void handleRoleIO(OVXSwitch vsw, OFVendor msg, Channel ch) {	
			log.debug("Tried to handle message \n{}\nwhile Switch {} not ACTIVE", 
					msg, vsw.getSwitchName());
		}

	}
	
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
	 * Used to save which channel the message came in on.
	 */
	private final XidTranslator<Channel> channelMux;

	/**
	 * Role Manager. Saves all role requests coming 
	 * from each controller. It is also responsible 
	 * for permitting or denying certain operations
	 * based on the current role of a controller.
	 */
	private final RoleManager roleMan;

	/** The component state of this OVXSwitch */
	protected SwitchState 						  state;
	
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
		this.capabilities = new OVXSwitchCapabilities();
		this.backOffCounter = new AtomicInteger();
		this.resetBackOff();
		this.bufferMap = new LRULinkedHashMap<Integer, OVXPacketIn>(
				OVXSwitch.bufferDimension);
		this.portCounter = new BitSetIndex(IndexType.PORT_ID);
		this.bufferId = new AtomicInteger(1);
		this.flowTable = new OVXFlowTable(this);
		this.roleMan = new RoleManager();
		this.channelMux = new XidTranslator<Channel>();
		// this.switchName = "OpenVirteX Virtual Switch 1.0";
		this.state = SwitchState.INIT;
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

	public boolean isActive() {
		return this.state.equals(SwitchState.ACTIVE);
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

	public void releasePortNumber(short portNumber) {
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
		this.state.register(this, physicalSwitches);
	}

	public void unregister() {
		/* calls unregSwitch() in OVXSwitch subclasses, which calls unregisterDP() */
		this.state.unregister(this);
	}
	
	/**
	 * Helper method for unregistering a switch  
	 * 
	 * @param synch synchronize with the Physical Switch that this 
	 * switch maps onto.   
	 */
	protected void unregisterDP(boolean synch) {
		DBManager.getInstance().remove(this);
		OVXNetwork net;
		try {
			net = this.getMap().getVirtualNetwork(this.tenantId);
		} catch (NetworkMappingException e) {
			log.error("Error retrieving the network with id {}. Unregister for OVXSwitch {} not fully done!", this.getTenantId(), this.getSwitchName());
			return;
		}
		if (this.getPorts() != null) {
			final Set<OVXPort> ports = 
					new HashSet<OVXPort>(this.getPorts().values());
			for (OVXPort port : ports) {
				port.tearDown();
				port.unregister();
			}
		}
		net.removeSwitch(this);
		/* if (synch), a PhysicalSwitch has already done this */
		if (!synch) {
			cleanUpFlowMods(false);
		}
		this.map.removeVirtualSwitch(this);
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
	public boolean tearDown() {
		return this.state.teardown(this, false);
	}
	
	/**
	 * teardown crossing virtual/physical boundary, called by a PhysicalSwitch
	 * when it's deregistered. 
	 * @param synch
	 * @return
	 */
	public boolean tearDown(boolean synch) {
		return this.state.teardown(this, synch);
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
		/* Call bootSwitch() in OVXSwitch subclass, which calls bootDP() */
		return this.state.boot(this);
	}
	
	/**
	 * Helper function for calling boot() routines for OVXSwitch. Called 
	 * by subclasses to run through boilerplate procedures. 
	 * @return
	 */
	protected boolean bootDP() {
		this.state = SwitchState.ACTIVE;
		for (OVXPort p : getPorts().values()) {
			if (p.isLink()) {
				p.boot();
			}	
		}
		this.generateFeaturesReply();
		final OpenVirteXController ovxController = OpenVirteXController
				.getInstance();
		ovxController.registerOVXSwitch(this);
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
				+ this.missSendLen + " - state: " + this.state
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
	 * .OFMessage)
	 */
	@Override
	public void handleIO(final OFMessage msg, Channel channel) {
		this.state.handleIO(this, msg, channel);
	}
	
	@Override
	public void handleRoleIO(OFVendor msg, Channel channel) {
		this.state.handleRoleIO(this, msg, channel);
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
		return this.flowTable.getFlowMod(cookie).clone();
	}
	
	public void setChannel(Channel channel) {
		this.roleMan.addController(channel);
	}
	
	public void removeChannel(Channel channel) {
		this.roleMan.removeChannel(channel);
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
	
	private Role extractNiciraRoleRequest(Channel chan,
			OFVendor vendorMessage) {
		int vendor = vendorMessage.getVendor();
		if (vendor != OFNiciraVendorData.NX_VENDOR_ID)
			return null;
		if (! (vendorMessage.getVendorData() instanceof OFRoleRequestVendorData))
			return null;
		OFRoleRequestVendorData roleRequestVendorData =
				(OFRoleRequestVendorData) vendorMessage.getVendorData();
		Role role = Role.fromNxRole(roleRequestVendorData.getRole());
		if (role == null) {
			String msg = String.format("Controller: [%s], State: [%s], "
					+ "received NX_ROLE_REPLY with invalid role "
					+ "value %d",
					chan.getRemoteAddress(),
					this.toString(),
					roleRequestVendorData.getRole());
			throw new ControllerStateException(msg);
		}
		return role;
	}
	
	private void denyAccess(Channel channel, OFMessage m, Role role) {
		log.warn("Controller {} may not send message {} because role state is {}", channel/*.getRemoteAddress()*/, m, role);
		OFMessage e = OVXMessageUtil.makeErrorMsg(OFBadRequestCode.OFPBRC_EPERM, m);
		channel.write(Collections.singletonList(e));
	}

	
	
	private void sendRoleReply(Role role, int xid, Channel channel) {
		OFVendor vendor = new OFVendor();
		vendor.setXid(xid);
		vendor.setVendor(OFNiciraVendorData.NX_VENDOR_ID);
		OFRoleReplyVendorData reply = new OFRoleReplyVendorData(role.toNxRole());
		vendor.setVendorData(reply);
		vendor.setLengthU(OFVendor.MINIMUM_LENGTH + reply.getLength());
		channel.write(Collections.singletonList(vendor));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.
	 * OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
	 * */
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		this.state.sendMsg(this, msg, from);
	}
	
	/**
	 * @param port
	 * @return true if this switch has route associated with specified port.
	 */
	public boolean hasRoute(OVXPort port) {
		return false;
	}
	
	@Override
	public void register() {
		/* refer to: register(final List<PhysicalSwitch> physicalSwitches) */ 
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
	
	/**
	 * Helper function calling the respective boot() of OVXSwitch subclasses.
	 */
	protected abstract boolean bootSwitch();

	/**
	 * Helper calling the respective unregister() of OVXSwitch subclasses.
	 * 
	 * @param synch true if called from Physical network, as in "synchronize"
	 */
	public abstract void unregSwitch(boolean synch);

}
