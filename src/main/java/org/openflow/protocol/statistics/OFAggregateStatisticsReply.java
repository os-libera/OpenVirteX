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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Represents an ofp_aggregate_stats_reply structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFAggregateStatisticsReply implements OFStatistics {
    protected long packetCount;
    protected long byteCount;
    protected int flowCount;

    /**
     * @return the packetCount
     */
    public long getPacketCount() {
        return this.packetCount;
    }

    /**
     * @param packetCount
     *            the packetCount to set
     */
    public void setPacketCount(final long packetCount) {
        this.packetCount = packetCount;
    }

    /**
     * @return the byteCount
     */
    public long getByteCount() {
        return this.byteCount;
    }

    /**
     * @param byteCount
     *            the byteCount to set
     */
    public void setByteCount(final long byteCount) {
        this.byteCount = byteCount;
    }

    /**
     * @return the flowCount
     */
    public int getFlowCount() {
        return this.flowCount;
    }

    /**
     * @param flowCount
     *            the flowCount to set
     */
    public void setFlowCount(final int flowCount) {
        this.flowCount = flowCount;
    }

    @Override
    @JsonIgnore
    public int getLength() {
        return 24;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        this.packetCount = data.readLong();
        this.byteCount = data.readLong();
        this.flowCount = data.readInt();
        data.readInt(); // pad
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        data.writeLong(this.packetCount);
        data.writeLong(this.byteCount);
        data.writeInt(this.flowCount);
        data.writeInt(0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 397;
        int result = 1;
        result = prime * result
                + (int) (this.byteCount ^ this.byteCount >>> 32);
        result = prime * result + this.flowCount;
        result = prime * result
                + (int) (this.packetCount ^ this.packetCount >>> 32);
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
        if (!(obj instanceof OFAggregateStatisticsReply)) {
            return false;
        }
        final OFAggregateStatisticsReply other = (OFAggregateStatisticsReply) obj;
        if (this.byteCount != other.byteCount) {
            return false;
        }
        if (this.flowCount != other.flowCount) {
            return false;
        }
        if (this.packetCount != other.packetCount) {
            return false;
        }
        return true;
    }
}
