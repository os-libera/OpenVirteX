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

import java.util.HashSet;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * @author gerola
 * 
 */

public class PhysicalNetwork extends
		Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

	private HashSet<Uplink> uplinkList;

	public HashSet<Uplink> getUplinkList() {
		return this.uplinkList;
	}

	public void setUplinkList(final HashSet<Uplink> uplinkList) {
		this.uplinkList = uplinkList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.network.Network#sendLLDP(java.lang.Object)
	 */
	@Override
	public void sendLLDP(final PhysicalSwitch sw) {
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
	public void receiveLLDP(final PhysicalSwitch sw) {
		// TODO Auto-generated method stub

	}

}
