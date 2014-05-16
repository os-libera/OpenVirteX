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
 * Represents an ofp_queue_stats structure
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueueStatisticsReply implements OFStatistics {
    protected short portNumber;
    protected int queueId;
    protected long transmitBytes;
    protected long transmitPackets;
    protected long transmitErrors;

    /**
     * @return the portNumber
     */
    public short getPortNumber() {
        return this.portNumber;
    }

    /**
     * @param portNumber
     *            the portNumber to set
     */
    public void setPortNumber(final short portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * @return the queueId
     */
    public int getQueueId() {
        return this.queueId;
    }

    /**
     * @param queueId
     *            the queueId to set
     */
    public void setQueueId(final int queueId) {
        this.queueId = queueId;
    }

    /**
     * @return the transmitBytes
     */
    public long getTransmitBytes() {
        return this.transmitBytes;
    }

    /**
     * @param transmitBytes
     *            the transmitBytes to set
     */
    public void setTransmitBytes(final long transmitBytes) {
        this.transmitBytes = transmitBytes;
    }

    /**
     * @return the transmitPackets
     */
    public long getTransmitPackets() {
        return this.transmitPackets;
    }

    /**
     * @param transmitPackets
     *            the transmitPackets to set
     */
    public void setTransmitPackets(final long transmitPackets) {
        this.transmitPackets = transmitPackets;
    }

    /**
     * @return the transmitErrors
     */
    public long getTransmitErrors() {
        return this.transmitErrors;
    }

    /**
     * @param transmitErrors
     *            the transmitErrors to set
     */
    public void setTransmitErrors(final long transmitErrors) {
        this.transmitErrors = transmitErrors;
    }

    @Override
    @JsonIgnore
    public int getLength() {
        return 32;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        this.portNumber = data.readShort();
        data.readShort(); // pad
        this.queueId = data.readInt();
        this.transmitBytes = data.readLong();
        this.transmitPackets = data.readLong();
        this.transmitErrors = data.readLong();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        data.writeShort(this.portNumber);
        data.writeShort((short) 0); // pad
        data.writeInt(this.queueId);
        data.writeLong(this.transmitBytes);
        data.writeLong(this.transmitPackets);
        data.writeLong(this.transmitErrors);
    }

    @Override
    public int hashCode() {
        final int prime = 439;
        int result = 1;
        result = prime * result + this.portNumber;
        result = prime * result + this.queueId;
        result = prime * result
                + (int) (this.transmitBytes ^ this.transmitBytes >>> 32);
        result = prime * result
                + (int) (this.transmitErrors ^ this.transmitErrors >>> 32);
        result = prime * result
                + (int) (this.transmitPackets ^ this.transmitPackets >>> 32);
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
        if (!(obj instanceof OFQueueStatisticsReply)) {
            return false;
        }
        final OFQueueStatisticsReply other = (OFQueueStatisticsReply) obj;
        if (this.portNumber != other.portNumber) {
            return false;
        }
        if (this.queueId != other.queueId) {
            return false;
        }
        if (this.transmitBytes != other.transmitBytes) {
            return false;
        }
        if (this.transmitErrors != other.transmitErrors) {
            return false;
        }
        if (this.transmitPackets != other.transmitPackets) {
            return false;
        }
        return true;
    }
}
