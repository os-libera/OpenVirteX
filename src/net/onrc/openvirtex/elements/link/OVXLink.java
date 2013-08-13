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

package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * The Class OVXLink.
 * 
 */
public class OVXLink extends Link<OVXPort,OVXSwitch> {

	/** The link id. */
	private Integer linkId;

	/** The tenant id. */
	private Integer tenantId;

	/**
	 * Instantiates a new virtual link.
	 */
	public OVXLink() {
		super();
		this.linkId = 0;
		this.tenantId = 0;
	}

	/**
	 * Instantiates a new virtual link.
	 * 
	 * @param srcPort
	 *            the source port
	 * @param dstPort
	 *            the destination port
	 * @param tenantId
	 *            the tenant id
	 * @param linkId
	 *            the link id
	 */
	public OVXLink(final OVXPort srcPort, final OVXPort dstPort,
			final Integer tenantId, final Integer linkId) {
		super(srcPort, dstPort);
		this.linkId = linkId;
		this.tenantId = tenantId;
	}

	/**
	 * Gets the link id.
	 * 
	 * @return the link id
	 */
	public Integer getLinkId() {
		return this.linkId;
	}

	/**
	 * Sets the link id.
	 * 
	 * @param linkId
	 *            the new link id
	 */
	public void setLinkId(final Integer linkId) {
		this.linkId = linkId;
	}

	/**
	 * Gets the tenant id.
	 * 
	 * @return the tenant id
	 */
	public Integer getTenantId() {
		return this.tenantId;
	}

	/**
	 * Sets the tenant id.
	 * 
	 * @param tenantId
	 *            the new tenant id
	 */
	public void setTenantId(final Integer tenantId) {
		this.tenantId = tenantId;
	}

}
