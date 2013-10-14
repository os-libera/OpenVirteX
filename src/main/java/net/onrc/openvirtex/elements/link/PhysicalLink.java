/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.link;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * The Class PhysicalLink.
 * 
 */
public class PhysicalLink extends Link<PhysicalPort, PhysicalSwitch> {

	
	private static AtomicInteger linkIds = new AtomicInteger(0);
	
	@SerializedName("linkId")
	@Expose
	private Integer linkId = null;
	
	
	/**
	 * Instantiates a new physical link.
	 * 
	 * @param srcPort
	 *            the source port
	 * @param dstPort
	 *            the destination port
	 */
	public PhysicalLink(final PhysicalPort srcPort, final PhysicalPort dstPort) {
		super(srcPort, dstPort);
		srcPort.setOutLink(this);
		dstPort.setInLink(this);
		this.linkId = PhysicalLink.linkIds.getAndIncrement();
	}
	
	
	public Integer getLinkId() {
		return linkId;
	}
	
	@Override
	public void unregister() {
		this.getSrcSwitch().getMap().removePhysicalLink(this);    
		srcPort.setOutLink(null);
		dstPort.setInLink(null);
	}
}
