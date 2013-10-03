/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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

package net.onrc.openvirtex.protocol;

import net.onrc.openvirtex.messages.OVXPacketIn;

import org.openflow.protocol.OFMatch;

public class OVXMatch extends OFMatch {

	private static final long serialVersionUID = 1L;
	protected long cookie;
	protected byte[] pktData;
	
	public OVXMatch() {
		super();
		this.cookie = 0;
		this.pktData = null;
	}
	

	/**
	 * @param match
	 */
	public OVXMatch(OFMatch match) {
		this.wildcards = match.getWildcards();
		this.inputPort = match.getInputPort();
		this.dataLayerSource = match.getDataLayerSource();
		this.dataLayerDestination = match.getDataLayerDestination();
		this.dataLayerVirtualLan = match.getDataLayerVirtualLan();
		this.dataLayerVirtualLanPriorityCodePoint = match.getDataLayerVirtualLanPriorityCodePoint();
		this.dataLayerType = match.getDataLayerType();
		this.networkTypeOfService = match.getNetworkTypeOfService();
		this.networkProtocol = match.getNetworkProtocol();
		this.networkSource = match.getNetworkSource();
		this.networkDestination = match.getNetworkDestination();
		this.transportSource = match.getTransportSource();
		this.transportDestination = match.getTransportDestination();
		this.cookie = 0;
		this.pktData = null;
	}


	/**
	 * Get cookie
	 * 
	 * @return
	 */
	public long getCookie() {
		return this.cookie;
	}

	/**
	 * Set cookie
	 * 
	 * @param cookie
	 */
	public OVXMatch setCookie(long cookie) {
		this.cookie = cookie;
		return this;
	}
	
	public byte[] getPktData() {
		return this.pktData;
	}


	public void setPktData(byte[] pktData) {
		this.pktData = pktData;
	}


	public boolean isFlowMod() {
		return this.cookie != 0;
	}
	
	public boolean isPacketOut() {
		return this.pktData != null;
	}
	
}
