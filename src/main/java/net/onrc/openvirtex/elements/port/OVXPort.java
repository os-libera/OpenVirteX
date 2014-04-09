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
package net.onrc.openvirtex.elements.port;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;
import net.onrc.openvirtex.messages.OVXPortStatus;
import net.onrc.openvirtex.elements.Persistable;

public class OVXPort extends Port<OVXSwitch, OVXLink> implements Persistable, Component {

	/**
	 * TODO for the love of ** figure out how to handle in/active vs portdown/up
	 */
	enum PortState {
		INIT {
			protected void register(OVXPort port) {	
				log.debug("registering port {}", port.toAP());
				port.pstate = PortState.INACTIVE;
				
				port.parentSwitch.addPort(port);
				port.physicalPort.setOVXPort(port);
				port.parentSwitch.generateFeaturesReply();
				DBManager.getInstance().save(port);
			}
		},
		INACTIVE {
			protected boolean boot(OVXPort port) {
				log.debug("enabling port {}", port.toAP());
				port.pstate = PortState.ACTIVE;
				
				/* send can only happen when parent switch is ACTIVE. */
				port.sendStatusMsg(OFPortReason.OFPPR_ADD);
				port.state = OFPortState.OFPPS_STP_FORWARD.getValue();
				
				port.sendStatusMsg(OFPortReason.OFPPR_MODIFY);
				
				/* isLink() only checks for OVXLinks */
				if (port.isLink()) { 
					OVXPort dst = port.portLink.egressLink.getDstPort();
					if (port.pstate.equals(dst.pstate)) {
						port.portLink.egressLink.boot();
						port.portLink.ingressLink.boot();
					}
				}
				/* attached hosts - is this true? */
				Host h = port.getHost();
				if (h != null) {
					h.boot();
					port.setEdge(true);
					port.physicalPort.setEdge(true);
				}
				/* fetch and synch routes if any */
				if (port.parentSwitch.hasRoute(port)) {
					for (SwitchRoute rt :
						((OVXBigSwitch)port.parentSwitch).getRoutebyPort(port)) {
						rt.boot();
					}
				}
				
				return true;
			}
			
			protected void unregister(OVXPort port) {
				log.debug("unregistering port {}", port.toAP());
				port.pstate = PortState.STOPPED;	
				
				DBManager.getInstance().remove(port);
				port.sendStatusMsg(OFPortReason.OFPPR_DELETE);
				/* Port may have links/routes/hosts associated with it, even if
				 * it didn't start that way. Both teardown() and unregister()
				 * elements since ACTIVE components may have been attached 
				 * to port while INACTIVE.*/
				if (port.isLink()){
					port.getLink().egressLink.tearDown();
					port.getLink().egressLink.unregister();
					port.getLink().ingressLink.tearDown();
					port.getLink().ingressLink.unregister();
				}
				Host h;
				if ((h = port.getHost()) != null){
					h.tearDown();
					h.unregister();
				}
				if (port.parentSwitch.hasRoute(port)) {
					for (SwitchRoute rt :
						((OVXBigSwitch)port.parentSwitch).getRoutebyPort(port)) {
						rt.tearDown();
						rt.unregister();
					}
				}
				
				port.parentSwitch.removePort(port.portNumber);
				port.physicalPort.removeOVXPort(port);
				port.parentSwitch.generateFeaturesReply();
				port.cleanUpFlowMods();
			}
		},
		ACTIVE {
			protected boolean teardown(OVXPort port) {
				log.debug("disabling port {}", port.toAP());
				port.pstate = PortState.INACTIVE;
				
				port.state |= OFPortState.OFPPS_LINK_DOWN.getValue();
				port.parentSwitch.generateFeaturesReply();
				port.sendStatusMsg(OFPortReason.OFPPR_MODIFY);
				
				/* tear down any links */
				if (port.isLink()) {
					port.portLink.egressLink.tearDown();
					port.portLink.ingressLink.tearDown();
				}
				/* hosts */
				Host h;
				if ((h = port.getHost()) != null) {
					h.tearDown();
				}
				/* routes, if any */
				if (port.parentSwitch.hasRoute(port)) {
					for (SwitchRoute rt :
						((OVXBigSwitch)port.parentSwitch).getRoutebyPort(port)) {
						// TODO remove if/when getRoutebyPort() takes backups into account. 
						if (rt != null) {
							rt.tearDown();
						}
					}
				}
				
				port.cleanUpFlowMods();
				return true;
			}	
		},
		STOPPED;
		
		protected void register(OVXPort port) {	
			log.warn("Cannot register port {} while status={}",
					port.toAP(), port.pstate);
		}
		
		protected boolean boot(OVXPort port) {
			log.warn("Cannot boot port {} while status={}",
					port.toAP(), port.pstate);
			return false;
		}
		
		protected boolean teardown(OVXPort port) {
			log.warn("Cannot teardown port {} while status={}", 
					port.toAP(), port.pstate);
			return false;
		}
		
		protected void unregister(OVXPort port) {
			log.warn("Cannot unregister port {} while status={}", 
					port.toAP(), port.pstate);
		}
		
	}
	
	private static Logger log = LogManager.getLogger(OVXPort.class
			.getName());

	private final Integer      tenantId;
	private final PhysicalPort physicalPort;
	private PortState pstate;
	
	public OVXPort(final int tenantId, final PhysicalPort port,
			final boolean isEdge, final short portNumber) throws IndexOutOfBoundException {
		super(port);
		this.tenantId = tenantId;
		this.physicalPort = port;
		try {		    
			this.parentSwitch = OVXMap.getInstance().getVirtualSwitch(
					port.getParentSwitch(), tenantId);
		} catch (SwitchMappingException e) {
			// something pretty wrong if we get here. Not 100% on how to handle this
			throw new RuntimeException("Unexpected state in OVXMap: " + e.getMessage());
		}
		this.portNumber = portNumber;
		this.name = "ovxport-"+this.portNumber;
		this.isEdge = isEdge;
		this.hardwareAddress = port.getHardwareAddress();
		PortFeatures features = new PortFeatures();
		features.setCurrentOVXPortFeatures();
		this.currentFeatures = features.getOVXFeatures();
		features.setAdvertisedOVXPortFeatures();
		this.advertisedFeatures = features.getOVXFeatures();
		features.setSupportedOVXPortFeatures();
		this.supportedFeatures = features.getOVXFeatures();
		features.setPeerOVXPortFeatures();
		this.peerFeatures = features.getOVXFeatures();
		this.state = OFPortState.OFPPS_LINK_DOWN.getValue();
		this.config = OFPortConfig.OFPPC_NO_STP.getValue();
		this.pstate = PortState.INIT;
	}

	public OVXPort(final int tenantId, final PhysicalPort port,
			final boolean isEdge)  throws IndexOutOfBoundException {
		this(tenantId, port, isEdge, (short) 0);
		this.portNumber = this.parentSwitch.getNextPortNumber();
		this.name = "ovxport-"+this.portNumber;
	}

	public Integer getTenantId() {
		return this.tenantId;
	}

	public PhysicalPort getPhysicalPort() {
		return this.physicalPort;
	}

	public Short getPhysicalPortNumber() {
		return this.physicalPort.getPortNumber();
	}

	/**
	 * TODO - eventually get rid of this by "locking" sendMsg()
	 * on port status. 
	 * @return
	 */
	public boolean isActive() {
		return this.pstate.equals(PortState.ACTIVE);
	}

	public boolean isLink() {
		return !this.isEdge; 
	}
	
	/**
	 * @return A host attached to the port, or null
	 */
	public Host getHost() {
		if (!this.isEdge) {
			return null;
		}
		try {
			OVXNetwork vnet = OVXMap.getInstance().getVirtualNetwork(this.tenantId);
			return vnet.getHost(this);
		} catch (NetworkMappingException e) {
			log.error("Port {} not associated with any tenants??", this.toAP());
			return null;
		}
	}
	
	// TODO Check if this ISN'T malformed. 
	private void sendStatusMsg(OFPortReason reason) {
		OFPortStatus status = new OFPortStatus();
		status.setDesc(this);
		status.setReason(reason.getReasonCode());
		this.parentSwitch.sendMsg(status, this.parentSwitch);
	}

	/**
	 * Registers a port in the virtual parent switch and in the physical port
	 */
	public void register() {
		this.pstate.register(this);
	}

	/**
	 * Modifies the fields of a OVXPortStatus message so that it is consistent  
	 * with the configs of the corresponding OVXPort. 
	 * 
	 * @param portstat
	 * @return
	 */
	public void virtualizePortStat(OVXPortStatus portstat) {
		OFPhysicalPort desc = portstat.getDesc();
		desc.setPortNumber(this.portNumber);
		desc.setHardwareAddress(this.hardwareAddress);
		desc.setCurrentFeatures(this.currentFeatures);
		desc.setAdvertisedFeatures(this.advertisedFeatures);
		desc.setSupportedFeatures(this.supportedFeatures);
		portstat.setDesc(desc);  
	}

	/**
	 * Changes the attribute of this port according to a MODIFY PortStatus
	 * @param portstat
	 */
	public void applyPortStatus(OVXPortStatus portstat) {
		if (portstat.getReason() != OFPortReason.OFPPR_MODIFY.getReasonCode()) {    	
			return;    
		}
		OFPhysicalPort psport = portstat.getDesc();
		this.config = psport.getConfig();    
		this.state = psport.getState();    
		this.peerFeatures = psport.getPeerFeatures();
	}

	public boolean boot() {
		return this.pstate.boot(this);
	}

	public boolean tearDown() {
		return this.pstate.teardown(this);
	}

	public void unregister() {
		this.pstate.unregister(this);
	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.tenantId);
		return index;
	}

	@Override
	public String getDBKey() {
		return Port.DB_KEY;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.putAll(this.getPhysicalPort().getDBObject());
		dbObject.put(TenantHandler.VPORT, this.portNumber);
		return dbObject;
	}

	private void cleanUpFlowMods() {
		log.info("Cleaning up flowmods for sw {} port {}", this.getPhysicalPort().getParentSwitch().getSwitchName(), this.getPhysicalPortNumber());
		this.getPhysicalPort().parentSwitch.
		cleanUpTenant(this.tenantId, this.getPhysicalPortNumber());		
	}

	public boolean equals(final OVXPort port) {
		return this.portNumber == port.portNumber
				&& this.parentSwitch.getSwitchId() == port.getParentSwitch()
				.getSwitchId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((physicalPort == null) ? 0 : physicalPort.hashCode());
		result = prime * result
				+ ((tenantId == null) ? 0 : tenantId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof OVXPort))
			return false;
		OVXPort other = (OVXPort) obj;
		if (physicalPort == null) {
			if (other.physicalPort != null)
				return false;
		} else if (!physicalPort.equals(other.physicalPort))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		return super.equals(obj);
	}

	@Override
	public String toString() {
		int linkId = 0;
		if (isLink())
			linkId = this.getLink().getOutLink().getLinkId();
		return "PORT:\n- portNumber: " + this.portNumber
				+ "\n- parentSwitch: " + this.getParentSwitch().getSwitchName()
				+ "\n- virtualNetwork: " + this.getTenantId()
				+ "\n- hardwareAddress: " + MACAddress.valueOf(this.hardwareAddress).toString()
				+ "\n- config: " + this.config + "\n- state: " + this.state
				+ "\n- currentFeatures: " + this.currentFeatures
				+ "\n- advertisedFeatures: " + this.advertisedFeatures
				+ "\n- supportedFeatures: " + this.supportedFeatures
				+ "\n- peerFeatures: " + this.peerFeatures 
				+ "\n- isEdge: " + this.isEdge
				+ "\n- state: " + this.pstate
				+ "\n- linkId: " + linkId
				+ "\n- physicalPortNumber: " + this.getPhysicalPortNumber()
				+ "\n- physicalSwitchName: " + this.getPhysicalPort().getParentSwitch().getSwitchName();
	}
}
