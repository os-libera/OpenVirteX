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

package net.onrc.openvirtex.elements.network;

import java.util.HashMap;

import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {

	// public VLinkManager vLinkMgmt;

	private int tenantId;
	private IPAddress network;
	private short mask;
	private HashMap<IPAddress, MACAddress> gwsMap;

	public int getTenantId() {
		return this.tenantId;
	}

	public void setTenantId(final int tenantId) {
		this.tenantId = tenantId;
	}

	public IPAddress getNetwork() {
		return this.network;
	}

	public void setNetwork(final IPAddress network) {
		this.network = network;
	}

	public short getMask() {
		return this.mask;
	}

	public void setMask(final short mask) {
		this.mask = mask;
	}

	public HashMap<IPAddress, MACAddress> getGwsMap() {
		return this.gwsMap;
	}

	public void setGwsMap(final HashMap<IPAddress, MACAddress> gwsMap) {
		this.gwsMap = gwsMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.network.Network#sendLLDP(java.lang.Object)
	 */
	@Override
	public void sendLLDP(final OVXSwitch sw) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.network.Network#receiveLLDP(java.lang.Object
	 * )
	 */
	@Override
	public void receiveLLDP(final OVXSwitch sw) {
		// TODO Auto-generated method stub

	}

}
