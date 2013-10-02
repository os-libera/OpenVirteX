/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFPhysicalPort;

/**
 * The Class Port.
 * 
 */
public class Port<T> extends OFPhysicalPort {

	protected MACAddress mac;
	protected Boolean isEdge;
	protected T parentSwitch;

	// TODO: duplexing/speed on port/link???

	/**
	 * Instantiates a new port.
	 */
	protected Port(final OFPhysicalPort ofPort) {
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

	@Override
	public void setHardwareAddress(final byte[] hardwareAddress) {
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
		return "PORT:\n- portNumber: " + this.portNumber
				+ "\n- hardwareAddress: " + this.hardwareAddress.toString()
				+ "\n- config: " + this.config + "\n- state: " + this.state
				+ "\n- currentFeatures: " + this.currentFeatures
				+ "\n- advertisedFeatures: " + this.advertisedFeatures
				+ "\n- supportedFeatures: " + this.supportedFeatures
				+ "\n- peerFeatures: " + this.peerFeatures + "\n- isEdge: "
				+ this.isEdge;
	}

}
