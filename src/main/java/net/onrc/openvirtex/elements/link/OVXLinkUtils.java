/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.link;

import java.util.BitSet;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.util.MACAddress;

/**
 * The Class OVXLinkUtils. This class provides some useful methods to
 * encapsulate/decapsulate the virtual link identifiers (tenantId, linkId,
 * flowId) inside the packet fields (mac addresses OR vlan)
 */
public class OVXLinkUtils {

    /** The tenant id. */
    private Integer    tenantId;

    /** The link id. */
    private Integer    linkId;

    /** The flow id. */
    private Integer    flowId;

    /** The src mac address. */
    private MACAddress srcMac;

    /** The dst mac address. */
    private MACAddress dstMac;

    /** The vlan. */
    private Short      vlan;

    /**
     * Instantiates a new link utils instance. Never called by eternal classes.
     */
    protected OVXLinkUtils() {
	this.tenantId = 0;
	this.linkId = 0;
	this.flowId = 0;
	this.srcMac = null;
	this.dstMac = null;
	this.vlan = 0;
    }

    /**
     * Bit set to int.
     * 
     * @param bitSet
     *            the bit set
     * @return the int
     */
    private static int bitSetToInt(final BitSet bitSet) {
	int bitInteger = 0;
	for (int i = 0; i < 32; i++) {
	    if (bitSet.get(i)) {
		bitInteger |= 1 << i;
	    }
	}
	return bitInteger;
    }

    /**
     * Instantiates a new link utils instance from the mac addresses couple.
     * Automatically decapsulate and set tenantId, linkId and flowId from the
     * params given.
     * 
     * @param srcMac
     *            the src mac
     * @param dstMac
     *            the dst mac
     */
    public OVXLinkUtils(final MACAddress srcMac, final MACAddress dstMac) {
	this();
	this.srcMac = srcMac;
	this.dstMac = dstMac;
	final int vNets = OpenVirteXController.getInstance()
	        .getNumberVirtualNets();
	final MACAddress mac = MACAddress
	        .valueOf((srcMac.toLong() & 0xFFFFFF) << 24 | dstMac.toLong()
	                & 0xFFFFFF);
	this.tenantId = (int) (mac.toLong() >> 48 - vNets);
	final BitSet bmask = new BitSet((48 - vNets) / 2);
	for (int i = bmask.nextClearBit(0); i < (48 - vNets) / 2; i = bmask
	        .nextClearBit(i + 1)) {
	    bmask.set(i);
	}
	final int mask = OVXLinkUtils.bitSetToInt(bmask);
	this.linkId = (int) (mac.toLong() >> (48 - vNets) / 2) & mask;
	this.flowId = (int) mac.toLong() & mask;
	this.vlan = 0;
    }

    /**
     * Instantiates a new link utils from tenantId, linkId and flowId.
     * Automatically encapsulate and set these values in the mac addresses and
     * in the vlan.
     * 
     * @param tenantId
     *            the tenant id
     * @param linkId
     *            the link id
     * @param flowId
     *            the flow id
     */
    public OVXLinkUtils(final Integer tenantId, final Integer linkId,
	    final Integer flowId) {
	this();
	this.tenantId = tenantId;
	this.linkId = linkId;
	this.flowId = flowId;
	final int vNets = OpenVirteXController.getInstance()
	        .getNumberVirtualNets();
	final MACAddress mac = MACAddress
	        .valueOf(tenantId.longValue() << 48 - vNets
	                | linkId.longValue() << (48 - vNets) / 2
	                | flowId.longValue());
	final Long src = mac.toLong() >> 24 & 0xFFFFFF;
	final Long dst = mac.toLong() & 0xFFFFFF;
	this.srcMac = MACAddress.valueOf((long) 0xa42305 << 24 | src);
	this.dstMac = MACAddress.valueOf((long) 0xa42305 << 24 | dst);
	// TODO: encapsulate the values in the vlan too
	this.vlan = 0;
    }

    /**
     * Checks if the utils instance is valid. To be valid, the instance has to
     * have tenantId, linkId and flowId set. Moreover, both mac addresses or the
     * vlan field has to be set too.
     * 
     * @return true, if is valid
     */
    public boolean isValid() {
	if (this.tenantId != 0 && this.linkId != 0 && this.flowId != 0) {
	    if (this.vlan != 0 || this.srcMac != null && this.dstMac != null) {
		return true;
	    }
	}
	return false;
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
     * Gets the link id.
     * 
     * @return the link id
     */
    public Integer getLinkId() {
	return this.linkId;
    }

    /**
     * Gets the flow id.
     * 
     * @return the flow id
     */
    public Integer getFlowId() {
	return this.flowId;
    }

    /**
     * Gets the src mac.
     * 
     * @return the src mac
     */
    public MACAddress getSrcMac() {
	return this.srcMac;
    }

    /**
     * Gets the dst mac.
     * 
     * @return the dst mac
     */
    public MACAddress getDstMac() {
	return this.dstMac;
    }

    /**
     * Gets the vlan.
     * 
     * @return the vlan
     */
    public Short getVlan() {
	return this.vlan;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "tenantId = " + this.tenantId + ", linkId = " + this.linkId
	        + ", flowId = " + this.flowId + ", srcMac = " + this.srcMac
	        + ", dstMac = " + this.dstMac + ", vlan = " + this.vlan;
    }

}
