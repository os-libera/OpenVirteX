/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBiMap;
import org.projectfloodlight.openflow.types.MacAddress;

public class OVXFlowManager {
    static Logger log = LogManager.getLogger(OVXFlowManager.class.getName());

    private final HashBiMap<Integer, BigInteger> flowValues;
    private final BitSetIndex flowCounter;
    private final Integer tenantId;
    private Collection<Host> hostList;

    public OVXFlowManager(Integer tenantId, Collection<Host> hostList) {
        this.flowValues = HashBiMap.create();
        this.flowCounter = new BitSetIndex(IndexType.FLOW_COUNTER);
        this.tenantId = tenantId;
        this.hostList = hostList;
    }

    public synchronized Integer storeFlowValues(final byte[] srcMac, final byte[] dstMac)
            throws IndexOutOfBoundException {
        // TODO: Optimize flow numbers
        final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
                dstMac));
        Integer flowId = this.flowValues.inverse().get(dualMac);
        if (flowId == null) {
            flowId = this.flowCounter.getNewIndex();
            log.debug(
                    "virtual net = {}: save flowId = {} that is associated to {} {}",
                    this.tenantId, flowId,
                    MacAddress.of(srcMac).toString(),
                    MacAddress.of(dstMac).toString());
            this.flowValues.put(flowId, dualMac);
        }
        return flowId;
    }

    public synchronized LinkedList<MacAddress> getFlowValues(final Integer flowId) {
        final LinkedList<MacAddress> macList = new LinkedList<MacAddress>();
        final BigInteger dualMac = this.flowValues.get(flowId);
        if (dualMac != null) {
            final MacAddress srcMac = MacAddress.of(dualMac.shiftRight(48).longValue());
            final MacAddress dstMac = MacAddress.of(dualMac.longValue());
            macList.add(srcMac);
            macList.add(dstMac);
        }
        return macList;
    }

    public synchronized Integer getFlowId(final byte[] srcMac, final byte[] dstMac)
            throws DroppedMessageException, IndexOutOfBoundException {
        final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
                dstMac));
        final Integer flowId = this.flowValues.inverse().get(dualMac);
        if (flowId != null && flowId != 0) {
            log.debug(
                    "virtual net = {}: retrieving flowId {} that is associated to {} {}",
                    this.tenantId, flowId,
                    MacAddress.of(srcMac).toString(),
                    MacAddress.of(dstMac).toString());
            return flowId;
        } else {
            // Create new flow ID
            // TODO: this is probably incorrect if the match is not identical
            // at both ends of the virtual link
            return this.storeFlowValues(srcMac, dstMac);
        }
    }

    /**
     * Gets list of all registered MAC addresses in this virtual network.
     */
    private List<MacAddress> getMACList() {
        final List<MacAddress> result = new LinkedList<MacAddress>();
        for (final Host host : this.hostList) {
            result.add(host.getMac());
        }
        return result;
    }

    public void boot() throws IndexOutOfBoundException {
        final List<MacAddress> macList = this.getMACList();
        for (final MacAddress srcMac : macList) {
            this.storeFlowValues(srcMac.getBytes(),
                    MacAddress.of("ff:ff:ff:ff:ff:ff").getBytes());
            for (final MacAddress dstMac : macList) {
                if (srcMac.getLong() != dstMac.getLong()) {
                    this.storeFlowValues(srcMac.getBytes(), dstMac.getBytes());
                }
            }
        }
    }
}
