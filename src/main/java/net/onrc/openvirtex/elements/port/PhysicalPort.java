/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
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
		return this.ovxPortMap.get(tenantId).get(vLinkId);
	}

	public void setOVXPort(final OVXPort ovxPort) {
		if (this.ovxPortMap.get(ovxPort.getTenantId()) != null) {
			this.ovxPortMap.get(ovxPort.getTenantId()).put(ovxPort.getLinkId(),
					ovxPort);
		} else {
			final HashMap<Integer, OVXPort> portMap = new HashMap<Integer, OVXPort>();
			portMap.put(ovxPort.getLinkId(), ovxPort);
			this.ovxPortMap.put(ovxPort.getTenantId(), portMap);
		}
	}
	
	public void removeOVXPort(OVXPort ovxPort) {
	    if (this.ovxPortMap.containsKey(ovxPort.getTenantId())) {
		this.ovxPortMap.remove(ovxPort.getTenantId());
	    }
	}

	public boolean equals(PhysicalPort port) {
	    return this.portNumber==port.portNumber && this.parentSwitch.getSwitchId() == port.getParentSwitch().getSwitchId();
	}
	    
	/**
	 * @return The OVXPorts that map to this PhysicalPort
	 */
	public Set<OVXPort> getOVXPorts() {
	    	// want a set of unique OVXPorts from map of maps.
	    	Set<OVXPort> ports = new HashSet<OVXPort>();
	    	for(HashMap<Integer, OVXPort> el : this.ovxPortMap.values()) {
	    	    	ports.addAll(el.values());
	    	}
	    	return ports;
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
}
