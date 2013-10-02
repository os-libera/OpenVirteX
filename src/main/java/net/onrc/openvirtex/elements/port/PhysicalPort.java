/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import org.openflow.protocol.OFPhysicalPort;

public class PhysicalPort extends Port<PhysicalSwitch> {

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
}
