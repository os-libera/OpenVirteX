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
public class PhysicalPort extends Port<PhysicalSwitch> {
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
	 * @param hardwareAddress
	 * @param config
	 * @param mask
	 * @param advertise
	 * @param isEdge
	 * @param parentSwitch
	 */
	public PhysicalPort(final short portNumber,
			final MACAddress hardwareAddress, final int config, final int mask,
			final int advertise, final Boolean isEdge,
			final PhysicalSwitch parentSwitch) {
		super(portNumber, hardwareAddress, config, mask, advertise, isEdge, parentSwitch);
		this.ovxPortMap = new HashMap<Integer, OVXPort>();
	}

	public PhysicalPort(PhysicalPort pp) {
		this(pp.portNumber, pp.hardwareAddress, pp.config, pp.mask,
				pp.advertise, pp.isEdge, pp.parentSwitch);
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

	public boolean updateOVXPort(final int tenantId, final OVXPort ovxPort) {
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
