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

package net.onrc.openvirtex.elements.port;

import java.util.HashMap;
import java.util.Map;

import org.openflow.protocol.OFPhysicalPort;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;

public class PhysicalPort extends Port<PhysicalSwitch> {

    private final Map<Integer, OVXPort> ovxPortMap;

    private PhysicalPort(OFPhysicalPort port) {
	super(port);
	this.ovxPortMap = new HashMap<Integer, OVXPort>();
    }
    
    /**
     * Instantiate PhysicalPort based on an OpenFlow physical port
     * @param port
     * @param sw
     */
    public PhysicalPort(OFPhysicalPort port, PhysicalSwitch sw, boolean isEdge) {
	this(port);
	this.parentSwitch = sw;
	this.isEdge = isEdge;
    }
    
    public OVXPort getOVXPort(final Integer tenantId) {
	return this.ovxPortMap.get(tenantId);
    }

    public void setOVXPort(final OVXPort ovxPort) {
	this.ovxPortMap.put(ovxPort.getTenantId(), ovxPort);
    }
    
    public boolean equals(PhysicalPort port) {
	if (this.portNumber==port.portNumber 
		&& this.parentSwitch.getSwitchId() == port.getParentSwitch().getSwitchId()) {
	    return true;
	} else {
	    return false;
	}
    }
}
