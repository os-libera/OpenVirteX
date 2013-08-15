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

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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
	
	private List<PhysicalLink> physicalLinks;

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
	public OVXLink(final Integer linkId, final Integer tenantId, final List<PhysicalLink> physicalLinks) {
		super();
		this.linkId = linkId;
		this.tenantId = tenantId;
		OVXPort srcPort = physicalLinks.get(0).getSrcPort().getOVXPort(this.tenantId);
		OVXPort dstPort = physicalLinks.get(physicalLinks.size() - 1).getDstPort().getOVXPort(this.tenantId);
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

	
	public HashMap<String,Object> toJson() {
		//HashMap<String,Object> output = new HashMap<String,Object>();
		//LinkedList<Object> list = new LinkedList<Object>();
		HashMap<String,Object> ovxMap = new HashMap<String,Object>();
		
		ovxMap.put("tenant-id",this.tenantId);
		ovxMap.put("link-id",this.getLinkId());
		ovxMap.put("src", this.getSrcPort().toJson());
		ovxMap.put("dst", this.getDstPort().toJson());
		
		//list.add(ovxMap);
		//output.put("edge", list);
		return ovxMap; 
	    }

}
