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
package net.onrc.openvirtex.elements.datapath;

import org.openflow.protocol.OFFeaturesReply.OFCapabilities;

/**
 * The Class OVXSwitchCapabilities.
 */
public class OVXSwitchCapabilities {

    /** The flow stats capability. */
    protected boolean flowStatsCapability = false;

    /** The table stats capability. */
    protected boolean tableStatsCapability = false;

    /** The port stats capability. */
    protected boolean portStatsCapability = false;

    /** The stp capability. */
    protected boolean stpCapability = false;

    /** The reassemble capability. */
    protected boolean reassembleCapability = false;

    /** The queue stats capability. */
    protected boolean queueStatsCapability = false;

    /** The match ip capability. */
    protected boolean matchIpCapability = false;

    /**
     * Instantiates a new oVX switch capabilities.
     */
    public OVXSwitchCapabilities() {
        this.flowStatsCapability = true;
        this.tableStatsCapability = true;
        this.portStatsCapability = true;
        this.stpCapability = false;
        this.reassembleCapability = false;
        this.queueStatsCapability = false;
        this.matchIpCapability = true;
    }

    /**
     * Sets the default capabilities.
     */
    public void setDefaultCapabilities() {
        this.flowStatsCapability = true;
        this.tableStatsCapability = true;
        this.portStatsCapability = true;
        this.stpCapability = false;
        this.reassembleCapability = false;
        this.queueStatsCapability = false;
        this.matchIpCapability = true;
    }

    /**
     * Gets the oVX switch capabilities.
     *
     * @return the oVX switch capabilities
     */
    public Integer getOVXSwitchCapabilities() {
        Integer capabilities = 0;
        if (this.flowStatsCapability) {
            capabilities += OFCapabilities.OFPC_FLOW_STATS.getValue();
        }
        if (this.tableStatsCapability) {
            capabilities += OFCapabilities.OFPC_TABLE_STATS.getValue();
        }
        if (this.portStatsCapability) {
            capabilities += OFCapabilities.OFPC_PORT_STATS.getValue();
        }
        if (this.stpCapability) {
            capabilities += OFCapabilities.OFPC_STP.getValue();
        }
        if (this.reassembleCapability) {
            capabilities += OFCapabilities.OFPC_IP_REASM.getValue();
        }
        if (this.queueStatsCapability) {
            capabilities += OFCapabilities.OFPC_QUEUE_STATS.getValue();
        }
        if (this.matchIpCapability) {
            capabilities += OFCapabilities.OFPC_ARP_MATCH_IP.getValue();
        }
        return capabilities;
    }

    /**
     * Checks if is flow stats capability.
     *
     * @return true, if is flow stats capability
     */
    public boolean isFlowStatsCapability() {
        return this.flowStatsCapability;
    }

    /**
     * Sets the flow stats capability.
     *
     * @param flowStatsCapability
     *            the new flow stats capability
     */
    public void setFlowStatsCapability(final boolean flowStatsCapability) {
        this.flowStatsCapability = flowStatsCapability;
    }

    /**
     * Checks if is table stats capability.
     *
     * @return true, if is table stats capability
     */
    public boolean isTableStatsCapability() {
        return this.tableStatsCapability;
    }

    /**
     * Sets the table stats capability.
     *
     * @param tableStatsCapability
     *            the new table stats capability
     */
    public void setTableStatsCapability(final boolean tableStatsCapability) {
        this.tableStatsCapability = tableStatsCapability;
    }

    /**
     * Checks if is port stats capability.
     *
     * @return true, if is port stats capability
     */
    public boolean isPortStatsCapability() {
        return this.portStatsCapability;
    }

    /**
     * Sets the port stats capability.
     *
     * @param portStatsCapability
     *            the new port stats capability
     */
    public void setPortStatsCapability(final boolean portStatsCapability) {
        this.portStatsCapability = portStatsCapability;
    }

    /**
     * Checks if is stp capability.
     *
     * @return true, if is stp capability
     */
    public boolean isStpCapability() {
        return this.stpCapability;
    }

    /**
     * Sets the stp capability.
     *
     * @param stpCapability
     *            the new stp capability
     */
    public void setStpCapability(final boolean stpCapability) {
        this.stpCapability = stpCapability;
    }

    /**
     * Checks if is reassemble capability.
     *
     * @return true, if is reassemble capability
     */
    public boolean isReassembleCapability() {
        return this.reassembleCapability;
    }

    /**
     * Sets the reassemble capability.
     *
     * @param reassembleCapability
     *            the new reassemble capability
     */
    public void setReassembleCapability(final boolean reassembleCapability) {
        this.reassembleCapability = reassembleCapability;
    }

    /**
     * Checks if is queue stats capability.
     *
     * @return true, if is queue stats capability
     */
    public boolean isQueueStatsCapability() {
        return this.queueStatsCapability;
    }

    /**
     * Sets the queue stats capability.
     *
     * @param queueStatsCapability
     *            the new queue stats capability
     */
    public void setQueueStatsCapability(final boolean queueStatsCapability) {
        this.queueStatsCapability = queueStatsCapability;
    }

    /**
     * Checks if is match ip capability.
     *
     * @return true, if is match ip capability
     */
    public boolean isMatchIpCapability() {
        return this.matchIpCapability;
    }

    /**
     * Sets the match ip capability.
     *
     * @param matchIpCapability
     *            the new match ip capability
     */
    public void setMatchIpCapability(final boolean matchIpCapability) {
        this.matchIpCapability = matchIpCapability;
    }

}
