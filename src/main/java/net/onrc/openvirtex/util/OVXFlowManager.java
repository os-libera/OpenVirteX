/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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

    public Integer storeFlowValues(final byte[] srcMac, final byte[] dstMac)
            throws IndexOutOfBoundException {
        // TODO: Optimize flow numbers
        final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
                dstMac));
        Integer flowId = this.flowValues.inverse().get(dualMac);
        if (flowId == null) {
            flowId = this.flowCounter.getNewIndex();
            log.debug(
                    "virtual net = {}: save flowId = {} that is associated to {} {}",
                    this.tenantId, flowId, MACAddress.valueOf(srcMac)
                            .toString(), MACAddress.valueOf(dstMac).toString());
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

    public Integer getFlowId(final byte[] srcMac, final byte[] dstMac)
            throws DroppedMessageException {
        final BigInteger dualMac = new BigInteger(ArrayUtils.addAll(srcMac,
                dstMac));
        final Integer flowId = this.flowValues.inverse().get(dualMac);
        if (flowId != null && flowId != 0) {
            log.debug(
                    "virtual net = {}: retrieving flowId {} that is associated to {} {}",
                    this.tenantId, flowId, MACAddress.valueOf(srcMac)
                            .toString(), MACAddress.valueOf(dstMac).toString());
            return flowId;
        }
        throw new DroppedMessageException(
                "virtual net =  "
                        + this.tenantId
                        + ": unable to retrive the flowId associated to these mac addresses: "
                        + MACAddress.valueOf(srcMac).toString() + "-"
                        + MACAddress.valueOf(dstMac).toString()
                        + ". Dropping message!");
    }

    /**
     * Gets list of all registered MAC addresses in this virtual network.
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
            this.storeFlowValues(srcMac.toBytes(),
                    MACAddress.valueOf("ff:ff:ff:ff:ff:ff").toBytes());
            for (final MACAddress dstMac : macList) {
                if (srcMac.toLong() != dstMac.toLong()) {
                    this.storeFlowValues(srcMac.toBytes(), dstMac.toBytes());
                }
            }
        }
    }
}
