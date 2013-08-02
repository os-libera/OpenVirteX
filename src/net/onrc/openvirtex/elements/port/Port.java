/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.elements.port;

import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFPhysicalPort;

/**
 * The Class Port.
 * 
 */
public class Port extends OFPhysicalPort implements Cloneable {

	// TODO: duplexing/speed on port/link???

	/** The is edge. */
	protected Boolean isEdge;

	/**
	 * Instantiates a new port.
	 */
	protected Port() {
	}

	/**
	 * Instantiates a new port.
	 * 
	 * @param portNumber
	 *            the port number
	 * @param hardwareAddress
	 *            the hardware address
	 */
	protected Port(final short portNumber, final MACAddress hwAddress,
			final boolean isEdge) {
		super();
		this.portNumber = portNumber;
		this.hardwareAddress = hwAddress.getAddress();
		this.name = "vport" + portNumber;
		this.config = 0;
		this.state = 0;
		this.currentFeatures = 0;
		this.advertisedFeatures = 0;
		this.supportedFeatures = 0;
		this.peerFeatures = 0;
		this.isEdge = isEdge;
	}

	/**
	 * Gets the checks if is edge.
	 * 
	 * @return the checks if is edge
	 */
	public Boolean getIsEdge() {
		return this.isEdge;
	}

	/**
	 * Sets the checks if is edge.
	 * 
	 * @param isEdge
	 *            the new checks if is edge
	 */
	public void setIsEdge(final Boolean isEdge) {
		this.isEdge = isEdge;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		// Log error throw exception
		throw new CloneNotSupportedException(
				"The base class should never be cloned.");
	}

	@Override
	public String toString() {
		return "PORT:\n- portNumber: " + this.portNumber + "\n- portName: "
				+ this.name + "\n- hardwareAddress: "
				+ MACAddress.valueOf(this.hardwareAddress) + "\n- isEdge: "
				+ this.isEdge + "\n- config: " + this.config + "\n- state: "
				+ this.state + "\n- currentFeatures: " + this.currentFeatures
				+ "\n- advertisedFeatures: " + this.advertisedFeatures
				+ "\n- supportedFeatures: " + this.supportedFeatures
				+ "\n- peerFeatures: " + this.peerFeatures;
	}

}
