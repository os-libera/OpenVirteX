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

package net.onrc.openvirtex.elements.link;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * The Class OVXLink.
 * 
 */
public class OVXLink extends Link<OVXPort, OVXSwitch> {

    /** The link id. */
    private final Integer            linkId;

    /** The tenant id. */
    private final Integer            tenantId;

    private final List<PhysicalLink> physicalLinks;

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
    public OVXLink(final Integer linkId, final Integer tenantId,
	    final List<PhysicalLink> physicalLinks) {
	super();
	this.linkId = linkId;
	this.tenantId = tenantId;
	final OVXPort srcPort = physicalLinks.get(0).getSrcPort()
	        .getOVXPort(this.tenantId);
	final OVXPort dstPort = physicalLinks.get(physicalLinks.size() - 1)
	        .getDstPort().getOVXPort(this.tenantId);
	super.srcPort = srcPort;
	super.dstPort = dstPort;
	this.physicalLinks = new LinkedList<PhysicalLink>(physicalLinks);
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
     * Gets the tenant id.
     * 
     * @return the tenant id
     */
    public Integer getTenantId() {
	return this.tenantId;
    }

    public void register() {
	OVXMap.getInstance().addLinks(this.physicalLinks, this);
    }

}
