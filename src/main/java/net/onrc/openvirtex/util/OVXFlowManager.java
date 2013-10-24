/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.util;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBiMap;

public class OVXFlowManager {
    static Logger                                log             = LogManager
            .getLogger(OVXFlowManager.class
                    .getName());
    
    private final HashBiMap<Integer, BigInteger> flowValues;
    private final BitSetIndex                    flowCounter;
    private final Integer tenantId;
    private List<Host> hostList;
    
    public OVXFlowManager(Integer tenantId, List<Host> hostList){
	this.flowValues = HashBiMap.create();
	this.flowCounter = new BitSetIndex(IndexType.FLOW_COUNTER);
	this.tenantId = tenantId;
	this.hostList = hostList;
    }
    
    public Integer storeFlowValues(final byte[] srcMac, final byte[] dstMac)
	    throws IndexOutOfBoundException {
	// TODO: Optimize flow numbers
	final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
	        dstMac));
	Integer flowId = this.flowValues.inverse().get(dualMac);
	if (flowId == null) {
	    flowId = this.flowCounter.getNewIndex();
	    log.debug("virtual net = {}: save flowId = {} that is associated to {} {}",
		            this.tenantId, flowId, MACAddress.valueOf(srcMac)
		                    .toString(), MACAddress.valueOf(dstMac)
		                    .toString());
	    this.flowValues.put(flowId, dualMac);
	}
	return flowId;
    }

    public LinkedList<MACAddress> getFlowValues(final Integer flowId) {
	final LinkedList<MACAddress> macList = new LinkedList<MACAddress>();
	final BigInteger dualMac = this.flowValues.get(flowId);
	if (dualMac != null) {
	    final MACAddress srcMac = MACAddress.valueOf(dualMac.shiftRight(48)
		    .longValue());
	    final MACAddress dstMac = MACAddress.valueOf(dualMac.longValue());
	    macList.add(srcMac);
	    macList.add(dstMac);
	}
	return macList;
    }

    public Integer getFlowId(final byte[] srcMac, final byte[] dstMac) {
	if (MACAddress.valueOf(srcMac).toLong() == 0 || MACAddress.valueOf(srcMac).toLong() == 0) {
	    log.warn("virtual net = {}: OVX doesn't store flowId associated to mac address == 00:00:00:00:00:00", this.tenantId);
	    return 0;
	}
	else {
	    final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
		    dstMac));
	    log.debug("virtual net = {}: retrieving flowId that is associated to {} {}",
		    this.tenantId, MACAddress.valueOf(srcMac).toString(),
		    MACAddress.valueOf(dstMac).toString());
	    final Integer flowId = this.flowValues.inverse().get(dualMac);
	    if (flowId == 0) {
		throw new VirtualLinkException("");
	    }
	    return flowId;
	}
    }

    /**
     * Get list of all registered MAC addresses in this virtual network
     */
    private List<MACAddress> getMACList() {
	final List<MACAddress> result = new LinkedList<MACAddress>();
	for (final Host host : this.hostList) {
	    result.add(host.getMac());
	}
	return result;
    }
    
    public void boot() throws IndexOutOfBoundException {
	final List<MACAddress> macList = this.getMACList();
	for (final MACAddress srcMac : macList) {
	    this.storeFlowValues(srcMac.toBytes(), MACAddress.valueOf("ff:ff:ff:ff:ff:ff").toBytes());
	    for (final MACAddress dstMac : macList) {
		if (srcMac.toLong() != dstMac.toLong()) {
		    this.storeFlowValues(srcMac.toBytes(), dstMac.toBytes());
		}
	    }
	}
    }
}
