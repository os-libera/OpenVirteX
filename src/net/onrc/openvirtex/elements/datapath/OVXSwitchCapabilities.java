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
package net.onrc.openvirtex.elements.datapath;

import org.openflow.protocol.OFFeaturesReply.OFCapabilities;

/**
 * The Class OVXSwitchCapabilities.
 */
public class OVXSwitchCapabilities {

    /** The flow stats capability. */
    protected boolean flowStatsCapability  = false;

    /** The table stats capability. */
    protected boolean tableStatsCapability = false;

    /** The port stats capability. */
    protected boolean portStatsCapability  = false;

    /** The stp capability. */
    protected boolean stpCapability        = false;

    /** The reassemble capability. */
    protected boolean reassembleCapability = false;

    /** The queue stats capability. */
    protected boolean queueStatsCapability = false;

    /** The match ip capability. */
    protected boolean matchIpCapability    = false;

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
