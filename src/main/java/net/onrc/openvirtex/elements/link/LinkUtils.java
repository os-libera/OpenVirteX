/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
/**
 * 
 */
package net.onrc.openvirtex.elements.link;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 *
 */
public class LinkUtils {
	private Integer tenantId;
	private Integer linkId;
	private Integer flowId;
	private MACAddress srcMac;
	private MACAddress dstMac;
	private Short vlan;
	
	public LinkUtils() {
		this.tenantId = 0;
		this.linkId = 0;
		this.flowId = 0;
		this.srcMac = null;
		this.dstMac = null;
		this.vlan = 0;
	}
	
	public static int bitSetToInt(BitSet bitSet)
	{
	    int bitInteger = 0;
	    for(int i = 0 ; i < 32; i++)
	        if(bitSet.get(i))
	            bitInteger |= (1 << i);
	    return bitInteger;
	}
	
	public LinkUtils(MACAddress srcMac, MACAddress dstMac) {
		this.srcMac = srcMac;
		this.dstMac = dstMac;
		int vNets = OpenVirteXController.getInstance().getNumberVirtualNets();
		MACAddress mac = MACAddress.valueOf(((srcMac.toLong() & 0xFFFFFF) << 24) | (dstMac.toLong() & 0xFFFFFF));
		this.tenantId = (int) (mac.toLong() >> (48-vNets));
		BitSet bmask = new BitSet((48-vNets)/2);
		for (int i = bmask.nextClearBit(0) ; i < (48-vNets)/2 ; i = bmask.nextClearBit(i+1) )
			bmask.set(i);
		int mask = bitSetToInt(bmask); 
		this.linkId = (int) (mac.toLong() >> ((48-vNets)/2)) & mask;
		this.flowId = (int) mac.toLong() & mask;
		this.vlan = 0;
	}
	
	public LinkUtils(Integer tenantId, Integer linkId, Integer flowId) {
		this.tenantId = tenantId;
		this.linkId = linkId;
		this.flowId = flowId;
		int vNets = OpenVirteXController.getInstance().getNumberVirtualNets();
		MACAddress mac = MACAddress.valueOf((tenantId.longValue()<<(48-vNets) | linkId.longValue()<<((48-vNets)/2) | flowId.longValue()));
		Long src = (mac.toLong() >> 24) & 0xFFFFFF;
		Long dst = mac.toLong() & 0xFFFFFF;
		this.srcMac = MACAddress.valueOf((((long)0xa42305) << 24) | src);
		this.dstMac = MACAddress.valueOf((((long)0xa42305) << 24) | dst);
		this.vlan = 0;
	}

	public boolean isValid() {
		if (this.tenantId != 0 && this.linkId != 0 && this.flowId !=0) {
			if (this.vlan != 0 || (this.srcMac != null && this.dstMac != null))
				return true;
		}
		return false;
	}
	
	public Integer getTenantId() {
		return tenantId;
	}

	public Integer getLinkId() {
		return linkId;
	}

	public Integer getFlowId() {
		return flowId;
	}

	public MACAddress getSrcMac() {
		return srcMac;
	}

	public MACAddress getDstMac() {
		return dstMac;
	}

	public Short getVlan() {
		return vlan;
	}
	
	public String toString() {
		return "tenantId = " + this.tenantId +
				", linkId = " + this.linkId +
				", flowId = " + this.flowId +
				", srcMac = " + this.srcMac +
				", dstMac = " + this.dstMac +
				", vlan = " + this.vlan;
	}
	
}
