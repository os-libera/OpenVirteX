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
import org.openflow.util.StringByteSerializer;

/**
 * Represents an ofp_table_stats structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFTableStatistics implements OFStatistics {
    public static int MAX_TABLE_NAME_LEN = 32;

    protected byte tableId;
    protected String name;
    protected int wildcards;
    protected int maximumEntries;
    protected int activeCount;
    protected long lookupCount;
    protected long matchedCount;

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
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the wildcards
     */
    public int getWildcards() {
        return this.wildcards;
    }

    /**
     * @param wildcards
     *            the wildcards to set
     */
    public void setWildcards(final int wildcards) {
        this.wildcards = wildcards;
    }

    /**
     * @return the maximumEntries
     */
    public int getMaximumEntries() {
        return this.maximumEntries;
    }

    /**
     * @param maximumEntries
     *            the maximumEntries to set
     */
    public void setMaximumEntries(final int maximumEntries) {
        this.maximumEntries = maximumEntries;
    }

    /**
     * @return the activeCount
     */
    public int getActiveCount() {
        return this.activeCount;
    }

    /**
     * @param activeCount
     *            the activeCount to set
     */
    public void setActiveCount(final int activeCount) {
        this.activeCount = activeCount;
    }

    /**
     * @return the lookupCount
     */
    public long getLookupCount() {
        return this.lookupCount;
    }

    /**
     * @param lookupCount
     *            the lookupCount to set
     */
    public void setLookupCount(final long lookupCount) {
        this.lookupCount = lookupCount;
    }

    /**
     * @return the matchedCount
     */
    public long getMatchedCount() {
        return this.matchedCount;
    }

    /**
     * @param matchedCount
     *            the matchedCount to set
     */
    public void setMatchedCount(final long matchedCount) {
        this.matchedCount = matchedCount;
    }

    @Override
    public int getLength() {
        return 64;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        this.tableId = data.readByte();
        data.readByte(); // pad
        data.readByte(); // pad
        data.readByte(); // pad
        this.name = StringByteSerializer.readFrom(data,
                OFTableStatistics.MAX_TABLE_NAME_LEN);
        this.wildcards = data.readInt();
        this.maximumEntries = data.readInt();
        this.activeCount = data.readInt();
        this.lookupCount = data.readLong();
        this.matchedCount = data.readLong();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        data.writeByte(this.tableId);
        data.writeByte((byte) 0); // pad
        data.writeByte((byte) 0); // pad
        data.writeByte((byte) 0); // pad
        StringByteSerializer.writeTo(data,
                OFTableStatistics.MAX_TABLE_NAME_LEN, this.name);
        data.writeInt(this.wildcards);
        data.writeInt(this.maximumEntries);
        data.writeInt(this.activeCount);
        data.writeLong(this.lookupCount);
        data.writeLong(this.matchedCount);
    }

    @Override
    public int hashCode() {
        final int prime = 449;
        int result = 1;
        result = prime * result + this.activeCount;
        result = prime * result
                + (int) (this.lookupCount ^ this.lookupCount >>> 32);
        result = prime * result
                + (int) (this.matchedCount ^ this.matchedCount >>> 32);
        result = prime * result + this.maximumEntries;
        result = prime * result
                + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + this.tableId;
        result = prime * result + this.wildcards;
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
        if (!(obj instanceof OFTableStatistics)) {
            return false;
        }
        final OFTableStatistics other = (OFTableStatistics) obj;
        if (this.activeCount != other.activeCount) {
            return false;
        }
        if (this.lookupCount != other.lookupCount) {
            return false;
        }
        if (this.matchedCount != other.matchedCount) {
            return false;
        }
        if (this.maximumEntries != other.maximumEntries) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.tableId != other.tableId) {
            return false;
        }
        if (this.wildcards != other.wildcards) {
            return false;
        }
        return true;
    }
}
