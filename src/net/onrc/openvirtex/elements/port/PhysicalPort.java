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

import java.util.HashMap;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class PhysicalPort extends Port {
	private final HashMap<Integer, OVXPort> ovxPortMap;
	private PhysicalSwitch parentSwitch;

	/**
     * 
     */
	public PhysicalPort() {
		super();
		this.ovxPortMap = new HashMap<Integer, OVXPort>();
		this.parentSwitch = null;
	}

	/**
	 * @param portNumber
	 * @param name
	 * @param hwAddress
	 * @param parentSwitch
	 * @param config
	 * @param state
	 * @param currentFeatures
	 * @param advertisedFeatures
	 * @param supportedFeatures
	 * @param peerFeatures
	 * @param isEdge
	 */
	public PhysicalPort(short portNumber, String name, byte[] hwAddress,
			PhysicalSwitch parentSwitch, Integer config, Integer state,
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
		this.ovxPortMap = new HashMap<Integer, OVXPort>();
		this.parentSwitch = parentSwitch;
		this.isEdge = isEdge;
		this.peerFeatures = peerFeatures;
	}

	/**
	 * @param portNumber
	 * @param name
	 * @param hwAddress
	 * @param parentSwitch
	 * @param config
	 * @param state
	 * @param currentFeatures
	 * @param advertisedFeatures
	 * @param supportedFeatures
	 * @param peerFeatures
	 * @param isEdge
	 */
	public PhysicalPort(short portNumber, String name, byte[] hwAddress,
			PhysicalSwitch parentSwitch, HashMap<Integer, OVXPort> ovxPortMap,
			Integer config, Integer state, Integer currentFeatures,
			Integer advertisedFeatures, Integer supportedFeatures,
			Integer peerFeatures, boolean isEdge) {
		super();
		this.portNumber = portNumber;
		this.name = name;
		this.hardwareAddress = hwAddress;
		this.config = config;
		this.state = state;
		this.currentFeatures = currentFeatures;
		this.advertisedFeatures = advertisedFeatures;
		this.supportedFeatures = supportedFeatures;
		this.ovxPortMap = ovxPortMap;
		this.parentSwitch = parentSwitch;
		this.isEdge = isEdge;
		this.peerFeatures = peerFeatures;
	}

	public PhysicalPort(PhysicalPort pp) {
		this(pp.portNumber, pp.name, pp.hardwareAddress, pp.parentSwitch,
				pp.ovxPortMap, pp.config, pp.state, pp.currentFeatures,
				pp.advertisedFeatures, pp.supportedFeatures, pp.peerFeatures,
				pp.isEdge);
	}

	public OVXPort getOVXPort(final int tenantId) {
		return this.ovxPortMap.get(tenantId);
	}

	public boolean setOVXPort(final int tenantId, final OVXPort ovxPort) {
		if (this.ovxPortMap.containsKey(tenantId)) {
			return false;
		} else {
			this.ovxPortMap.put(tenantId, ovxPort);
		}
		return true;
	}

	public Short getOVXPortNumber(Integer tenantId) {
		return this.ovxPortMap.get(tenantId).getPortNumber();
	}

	public boolean updateOVXPort(final int tenantId, final OVXPort ovxPort) {
		if (!this.ovxPortMap.containsKey(tenantId)) {
			return false;
		} else {
			this.ovxPortMap.put(tenantId, ovxPort);
		}
		return true;
	}

	public PhysicalSwitch getParentSwitch() {
		return this.parentSwitch;
	}

	public boolean setParentSwitch(final PhysicalSwitch parentSwitch) {
		this.parentSwitch = parentSwitch;
		return true;
	}

	@Override
	public PhysicalPort clone() {
		return new PhysicalPort(this);
	}

	@Override
	public String toString() {
		return "PORT:\n- portNumber: " + this.portNumber + "\n- portName: "
				+ this.name + "\n- hardwareAddress: "
				+ MACAddress.valueOf(this.hardwareAddress) + "\n- isEdge: "
				+ this.isEdge + "\n- parentSwitch: "
				+ this.parentSwitch.getSwitchName() + "\n- config: "
				+ this.config + "\n- state: " + this.state
				+ "\n- currentFeatures: " + this.currentFeatures
				+ "\n- advertisedFeatures: " + this.advertisedFeatures
				+ "\n- supportedFeatures: " + this.supportedFeatures
				+ "\n- peerFeatures: " + this.peerFeatures;
	}
}
