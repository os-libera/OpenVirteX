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
/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.openflow.protocol.statistics;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.protocol.OFMatch;

/**
 * Represents an ofp_flow_stats_request structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFFlowStatisticsRequest implements OFStatistics {
    protected OFMatch match;
    protected byte tableId;
    protected short outPort;

    /**
     * @return the match
     */
    public OFMatch getMatch() {
        return this.match;
    }

    /**
     * @param match
     *            the match to set
     */
    public void setMatch(final OFMatch match) {
        this.match = match;
    }

    /**
     * @return the tableId
     */
    public byte getTableId() {
        return this.tableId;
    }

    /**
     * @param tableId
     *            the tableId to set
     */
    public void setTableId(final byte tableId) {
        this.tableId = tableId;
    }

    /**
     * @return the outPort
     */
    public short getOutPort() {
        return this.outPort;
    }

    /**
     * @param outPort
     *            the outPort to set
     */
    public void setOutPort(final short outPort) {
        this.outPort = outPort;
    }

    @Override
    public int getLength() {
        return 44;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        if (this.match == null) {
            this.match = new OFMatch();
        }
        this.match.readFrom(data);
        this.tableId = data.readByte();
        data.readByte(); // pad
        this.outPort = data.readShort();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        this.match.writeTo(data);
        data.writeByte(this.tableId);
        data.writeByte((byte) 0);
        data.writeShort(this.outPort);
    }

    @Override
    public int hashCode() {
        final int prime = 421;
        int result = 1;
        result = prime * result
                + (this.match == null ? 0 : this.match.hashCode());
        result = prime * result + this.outPort;
        result = prime * result + this.tableId;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFFlowStatisticsRequest)) {
            return false;
        }
        final OFFlowStatisticsRequest other = (OFFlowStatisticsRequest) obj;
        if (this.match == null) {
            if (other.match != null) {
                return false;
            }
        } else if (!this.match.equals(other.match)) {
            return false;
        }
        if (this.outPort != other.outPort) {
            return false;
        }
        if (this.tableId != other.tableId) {
            return false;
        }
        return true;
    }
}
