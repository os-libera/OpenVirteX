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

import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public class OVXLink extends Link<OVXPort> {
    private int linkId;
    private int tenantId;

    /**
     * 
     */
    public OVXLink() {
	super();
	this.linkId = 0;
	this.tenantId = 0;
    }

    /**
     * @param srcPort
     * @param dstPort
     * @param tenantId
     * @param linkId
     */
    public OVXLink(final OVXPort srcPort, final OVXPort dstPort,
	    final int tenantId, final int linkId) {
	super(srcPort, dstPort);
	this.linkId = linkId;
	this.tenantId = tenantId;
    }

    public int getLinkId() {
	return this.linkId;
    }

    public void setLinkId(final int linkId) {
	this.linkId = linkId;
    }

    public int getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final int tenantId) {
	this.tenantId = tenantId;
    }

}
