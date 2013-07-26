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

package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.OVXPort;

public abstract class OVXSwitch extends Switch<OVXPort> {
	private int tenantId;
	private short pktLenght;

	/**
     * 
     */
	public OVXSwitch() {
		super();
		this.tenantId = 0;
		this.pktLenght = 0;
	}

	/**
	 * @param switchName
	 * @param switchId
	 * @param map
	 */
	public OVXSwitch(final String switchName, final long switchId,
			final OVXMap map, final int tenantId, final short pktLenght) {
		super(switchName, switchId, map);
		this.tenantId = tenantId;
		this.pktLenght = pktLenght;
	}

	public int getTenantId() {
		return this.tenantId;
	}

	public boolean setTenantId(final int tenantId) {
		this.tenantId = tenantId;
		return true;
	}

	public short getPktLenght() {
		return this.pktLenght;
	}

	public boolean setPktLenght(final short pktLenght) {
		this.pktLenght = pktLenght;
		return true;
	}

}
