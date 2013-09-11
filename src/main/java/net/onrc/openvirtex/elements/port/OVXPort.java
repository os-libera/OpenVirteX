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

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;

public class OVXPort extends Port<OVXSwitch> {

    private final Integer      tenantId;
    private final PhysicalPort physicalPort;
    /** The link id. */
    private Integer linkId;

    public OVXPort(final int tenantId, final PhysicalPort port,
	    final boolean isEdge) {
	super(port);
	this.tenantId = tenantId;
	this.physicalPort = port;
	this.parentSwitch = OVXMap.getInstance().getVirtualSwitch(
	        port.getParentSwitch(), tenantId);
	this.portNumber = this.parentSwitch.getNextPortNumber();
	this.hardwareAddress = port.getHardwareAddress();
	this.isEdge = isEdge;
	// advertise current speed/duplex as 100MB FD, media type copper
	this.currentFeatures = 324;
	// advertise current feature + 1GB FD
	this.advertisedFeatures = 340;
	// advertise all the OF1.0 physical port speeds and duplex. Advertise
	// media type as copper
	this.supportedFeatures = 383;
	this.linkId = 0;
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

    public Integer getLinkId() {
        return linkId;
    }

    public void setLinkId(Integer linkId) {
        this.linkId = linkId;
    }
    
    /**
     * Registers a port in the virtual parent switch and in the physical port
     */
    public void register() {
	this.parentSwitch.addPort(this);
	this.physicalPort.setOVXPort(this);
    }

    @Override
    public String toString() {
	String result = super.toString();
	result += "\n- tenantId: " + this.tenantId + "\n- linkId: " + this.linkId;
	return result;
    }
}
