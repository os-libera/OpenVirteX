/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.messages.OVXPortStatus;

import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;


public class PhysicalPort extends Port<PhysicalSwitch, PhysicalLink> {

	private final Map<Integer, HashMap<Integer, OVXPort>> ovxPortMap;
	
	private PhysicalPort(final OFPhysicalPort port) {
		super(port);
		this.ovxPortMap = new HashMap<Integer, HashMap<Integer, OVXPort>>();
	}

	/**
	 * Instantiate PhysicalPort based on an OpenFlow physical port
	 * 
	 * @param port
	 * @param sw
	 */
	public PhysicalPort(final OFPhysicalPort port, final PhysicalSwitch sw,
			final boolean isEdge) {
		this(port);
		this.parentSwitch = sw;
		this.isEdge = isEdge;
	}

	public OVXPort getOVXPort(final Integer tenantId, final Integer vLinkId) {
		if (this.ovxPortMap.get(tenantId) == null) {
			return null;
		}
		OVXPort p = this.ovxPortMap.get(tenantId).get(vLinkId);
		if (p != null && !p.isActive())
			return null;
		return p;
	}

	public void setOVXPort(final OVXPort ovxPort) {
		if (this.ovxPortMap.get(ovxPort.getTenantId()) != null) {
		    if (ovxPort.getLink() != null)
			this.ovxPortMap.get(ovxPort.getTenantId()).put(ovxPort.getLink().getInLink().getLinkId(),
					ovxPort);
		    else 
			this.ovxPortMap.get(ovxPort.getTenantId()).put(0,ovxPort);
		} else {
			final HashMap<Integer, OVXPort> portMap = new HashMap<Integer, OVXPort>();
			if (ovxPort.getLink() != null)
			    portMap.put(ovxPort.getLink().getInLink().getLinkId(), ovxPort);
			else 
			    portMap.put(0, ovxPort);
			this.ovxPortMap.put(ovxPort.getTenantId(), portMap);
		}
	}

	@Override
	public Map<String, Object> getDBIndex() {
		return null;
	}

	@Override
	public String getDBKey() {
		return null;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.put(TenantHandler.DPID, this.getParentSwitch().getSwitchId());
		dbObject.put(TenantHandler.PORT, this.portNumber);
		return dbObject;
	}
	
	public void removeOVXPort(OVXPort ovxPort) {
	    if (this.ovxPortMap.containsKey(ovxPort.getTenantId())) {
		this.ovxPortMap.remove(ovxPort.getTenantId());
	    }
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;
		if (this == that)
			return true;
		if (!(that instanceof PhysicalPort))
			return false;
		
		PhysicalPort port = (PhysicalPort) that;
	    return this.portNumber==port.portNumber 
	    		&& this.parentSwitch.getSwitchId() == port.getParentSwitch().getSwitchId();
	}
	    
	/**
	 * @param tenant The ID of the tenant of interest
	 * @return The OVXPorts that map to this PhysicalPort for a given tenant ID, if 
	 * tenant is null all of the OVXPorts mapping to this port
	 */
	public List<Map<Integer, OVXPort>> getOVXPorts(Integer tenant) {
	    	List<Map<Integer, OVXPort>> ports = new ArrayList<Map<Integer,OVXPort>>();
		if (tenant == null) {    	
		    	ports.addAll(this.ovxPortMap.values());
	    	} else {
			ports.add(this.ovxPortMap.get(tenant));
		}
	    	return Collections.unmodifiableList(ports);
	}
	
	/**
	 * Changes the attribute of this port according to a MODIFY PortStatus
	 * @param portstat
	 */
	public void applyPortStatus(OVXPortStatus portstat) {
		if (!portstat.isReason(OFPortReason.OFPPR_MODIFY)) {    	
			return;    
		}
		OFPhysicalPort psport = portstat.getDesc();
		this.portNumber = psport.getPortNumber();
		this.hardwareAddress = psport.getHardwareAddress();
		this.name = psport.getName();
		this.config = psport.getConfig();    
		this.state = psport.getState();    
		this.currentFeatures = psport.getCurrentFeatures();
		this.advertisedFeatures = psport.getAdvertisedFeatures();
		this.supportedFeatures = psport.getSupportedFeatures();
		this.peerFeatures = psport.getPeerFeatures();
	}

	/**
	 * unmaps this port from the global mapping and its parent switch. 
	 */
	public void unregister() {
		/* remove links, if any */
		if ((this.portLink != null) && (this.portLink.exists())) {
			this.portLink.egressLink.unregister();
			this.portLink.ingressLink.unregister();
		}
	}
}
