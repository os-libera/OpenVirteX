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

import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFPhysicalPort;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The Class Port.
 * 
 */
public class Port<T> extends OFPhysicalPort {

    protected MACAddress mac;
    protected Boolean    isEdge;
    @SerializedName("dpid")
    @Expose
    protected T          parentSwitch;
    // TODO: check if we didn't break anything by redeclaring the portNumber
    @SerializedName("port")
    @Expose
    protected short portNumber;

    // TODO: duplexing/speed on port/link???

    /**
     * Instantiates a new port.
     */
    protected Port(OFPhysicalPort ofPort) {
	super();
	this.portNumber = ofPort.getPortNumber();
	this.hardwareAddress = ofPort.getHardwareAddress();
	this.name = ofPort.getName();
	this.config = ofPort.getConfig();
	this.state = ofPort.getState();
	this.currentFeatures = ofPort.getCurrentFeatures();
	this.advertisedFeatures = ofPort.getAdvertisedFeatures();
	this.supportedFeatures = ofPort.getSupportedFeatures();
	this.peerFeatures = ofPort.getPeerFeatures();	
	this.mac = null;
	this.isEdge = false;
	this.parentSwitch = null;
    }
    
    public void setHardwareAddress(byte[] hardwareAddress) {
	super.setHardwareAddress(hardwareAddress);
	// no way to update MACAddress instances
	this.mac = new MACAddress(hardwareAddress);
    }
    
    /**
     * Gets the checks if is edge.
     * 
     * @return the checks if is edge
     */
    public Boolean isEdge() {
	return this.isEdge;
    }


    /**
     * Sets the checks if is edge.
     * 
     * @param isEdge
     *            the new checks if is edge
     */
    public void isEdge(final Boolean isEdge) {
	this.isEdge = isEdge;
    }

    public T getParentSwitch() {
	return this.parentSwitch;
    }

    @Override
    public String toString() {
	return "PORT:\n- portNumber: " + this.portNumber +
		"\n- hardwareAddress: " + this.hardwareAddress.toString() +
		"\n- config: " + this.config +
		"\n- state: " + this.state +
		"\n- currentFeatures: " + this.currentFeatures +
	        "\n- advertisedFeatures: " + this.advertisedFeatures +
	        "\n- supportedFeatures: " + this.supportedFeatures +
	        "\n- peerFeatures: " + this.peerFeatures +
		"\n- isEdge: " + this.isEdge;
    }

}
