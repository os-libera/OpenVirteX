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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFPhysicalPort;

public class OVXPort extends Port<OVXSwitch> {

    private Integer      tenantId;
    private PhysicalPort physicalPort;

    /**
     * 
     */
    public OVXPort() {
	super();
	this.tenantId = -1;
	this.physicalPort = null;
	this.makeVirtual();
    }

    public OVXPort(final OVXPort vp) {
	super((OFPhysicalPort) vp, vp.isEdge, vp.parentSwitch);
	this.tenantId = -1;
	this.physicalPort = null;
    }

    public OVXPort(final OVXPort vp, Integer tenantId, PhysicalPort physicalPort) {
	this(vp);
	this.tenantId = tenantId;
	this.physicalPort = physicalPort;
    }
    
    public OVXPort(final Short portNumber, final byte[] hardwareAddress, final Boolean isEdge,
	    final OVXSwitch parentSwitch, final Integer tenantId) {
	super(portNumber, hardwareAddress, "vport", 0, 0, 0, 0, 0, 0, isEdge, parentSwitch);
	this.tenantId = tenantId;
    }
    
    private void makeVirtual() {
	// advertise current speed/duplex as 100MB FD, media type copper
	this.currentFeatures = 324;
	// advertise current feature + 1GB FD
	this.advertisedFeatures = 340;
	// advertise all the OF1.0 physical port speeds and duplex. Advertise
	// media type as copper
	this.supportedFeatures = 383;	
    }

    public Integer getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final Integer tenantId) {
	this.tenantId = tenantId;
    }

    public PhysicalPort getPhysicalPort() {
	return this.physicalPort;
    }

    public boolean setPhysicalPort(final PhysicalPort physicalPort) {
	if (this.physicalPort != null) {
	    return false;
	} else {
	    this.physicalPort = physicalPort;
	    return true;
	}
    }

    public Short getPhysicalPortNumber() {
	return this.physicalPort.getPortNumber();
    }

    boolean updatePhysicalPort(final PhysicalPort physicalPort) {
	if (this.physicalPort == null) {
	    return false;
	} else {
	    this.physicalPort = physicalPort;
	    return true;
	}
    }

    @Override
    public OVXPort clone() {
	return new OVXPort(this);
    }

    @Override
    public String toString() {
	String result = super.toString();
	result += "\n- tenantId: " + this.tenantId;
	return result;
    }
}
