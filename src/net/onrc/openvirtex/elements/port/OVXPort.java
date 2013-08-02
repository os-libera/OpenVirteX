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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class OVXPort extends Port {
	private Integer tenantId;
	private PhysicalPort physicalPort;
	private OVXSwitch parentSwitch;

	/**
     * 
     */
	public OVXPort() {
		super();
		this.tenantId = 0;
		this.physicalPort = null;
		this.parentSwitch = null;
	}

	/**
	 * @param portNumber
	 * @param hardwareAddress
	 * @param config
	 * @param mask
	 * @param advertise
	 * @param isEdge
	 * @param tenantId
	 * @param physicalPort
	 * @param parentSwitch
	 */
	public OVXPort(final short portNumber, final MACAddress hardwareAddress,
			Boolean isEdge, final Integer tenantId, final OVXSwitch parentSwitch) {
		super(portNumber, hardwareAddress, isEdge);

		// advertise current speed/duplex as 100MB FD, media type copper
		this.currentFeatures = 324;
		// advertise current feature + 1GB FD
		this.advertisedFeatures = 340;
		// advertise all the OF1.0 physical port speeds and duplex. Advertise
		// media type as copper
		this.supportedFeatures = 383;

		this.tenantId = tenantId;
		this.physicalPort = null;
		this.parentSwitch = parentSwitch;
	}

	/**
	 * @param portNumber
	 * @param hwAddress
	 * @param tenantId
	 * @param physicalPort
	 * @param parentSwitch
	 * @param config
	 * @param state
	 * @param currentFeatures
	 * @param advertisedFeatures
	 * @param supportedFeatures
	 */
	public OVXPort(short portNumber, MACAddress hwAddress, Integer tenantId,
			PhysicalPort physicalPort, OVXSwitch parentSwitch, Integer config,
			Integer state, Integer currentFeatures, Integer advertisedFeatures,
			Integer supportedFeatures, boolean isEdge) {
		super(portNumber, hwAddress, isEdge);
		this.config = config;
		this.state = state;
		this.currentFeatures = currentFeatures;
		this.advertisedFeatures = advertisedFeatures;
		this.supportedFeatures = supportedFeatures;
		this.tenantId = tenantId;
		this.physicalPort = physicalPort;
		this.parentSwitch = parentSwitch;
	}

	/**
	 * @param portNumber
	 * @param name
	 * @param hwAddress
	 * @param tenantId
	 * @param physicalPort
	 * @param parentSwitch
	 * @param config
	 * @param state
	 * @param currentFeatures
	 * @param advertisedFeatures
	 * @param supportedFeatures
	 * @param peerFeatures
	 * @param isEdge
	 */
	public OVXPort(short portNumber, String name, byte[] hwAddress,
			Integer tenantId, PhysicalPort physicalPort,
			OVXSwitch parentSwitch, Integer config, Integer state,
			Integer currentFeatures, Integer advertisedFeatures,
			Integer supportedFeatures, Integer peerFeatures, boolean isEdge) {
		super();
		this.portNumber = portNumber;
		this.name = name;
		this.hardwareAddress = hwAddress;
		this.config = config;
		this.state = state;
		this.currentFeatures = currentFeatures;
		this.advertisedFeatures = advertisedFeatures;
		this.supportedFeatures = supportedFeatures;
		this.tenantId = tenantId;
		this.physicalPort = physicalPort;
		this.parentSwitch = parentSwitch;
		this.isEdge = isEdge;
		this.peerFeatures = peerFeatures;
	}

	public OVXPort(OVXPort op) {
		this(op.portNumber, op.name, op.hardwareAddress, op.tenantId,
				op.physicalPort, op.parentSwitch, op.config, op.state,
				op.currentFeatures, op.advertisedFeatures,
				op.supportedFeatures, op.peerFeatures, op.isEdge);
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

	public OVXSwitch getParentSwitch() {
		return this.parentSwitch;
	}

	public void setParentSwitch(final OVXSwitch parentSwitch) {
		this.parentSwitch = parentSwitch;
	}

	@Override
	public OVXPort clone() {
		return new OVXPort(this);
	}

	@Override
	public String toString() {
		return "PORT:\n- portNumber: " + this.portNumber + "\n- portName: "
				+ this.name + "\n- hardwareAddress: "
				+ MACAddress.valueOf(this.hardwareAddress) + "\n- isEdge: "
				+ this.isEdge + "\n- tenantId: " + this.tenantId
				+ "\n- parentSwitch: " + this.parentSwitch.getSwitchName()
				+ "\n- config: " + this.config + "\n- state: " + this.state
				+ "\n- currentFeatures: " + this.currentFeatures
				+ "\n- advertisedFeatures: " + this.advertisedFeatures
				+ "\n- supportedFeatures: " + this.supportedFeatures
				+ "\n- peerFeatures: " + this.peerFeatures;
	}
}
