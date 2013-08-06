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

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

public class PhysicalPort extends Port<PhysicalSwitch> {

    private final HashMap<Integer, OVXPort> ovxPortMap;

    /**
     * 
     */
    public PhysicalPort() {
	super();
	this.ovxPortMap = new HashMap<Integer, OVXPort>();
    }

    // TODO: do we need constructor with ovxPortMap?

    public PhysicalPort(final PhysicalPort pp) {
	super(pp.portNumber, pp.hardwareAddress.getAddress(), pp.isEdge,
	        pp.parentSwitch, pp.config, pp.state, pp.currentFeatures,
	        pp.advertisedFeatures, pp.supportedFeatures, pp.peerFeatures);
	this.ovxPortMap = new HashMap<Integer, OVXPort>();
    }

    public OVXPort getOVXPort(final Integer tenantId) {
	return this.ovxPortMap.get(tenantId);
    }

    public boolean setOVXPort(final Integer tenantId, final OVXPort ovxPort) {
	if (this.ovxPortMap.containsKey(tenantId)) {
	    return false;
	} else {
	    this.ovxPortMap.put(tenantId, ovxPort);
	}
	return true;
    }

    public Short getOVXPortNumber(final Integer tenantId) {
	return this.ovxPortMap.get(tenantId).getPortNumber();
    }

    public boolean updateOVXPort(final Integer tenantId, final OVXPort ovxPort) {
	if (!this.ovxPortMap.containsKey(tenantId)) {
	    return false;
	} else {
	    this.ovxPortMap.put(tenantId, ovxPort);
	}
	return true;
    }

    @Override
    public PhysicalPort clone() {
	return new PhysicalPort(this);
    }
}
