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

package net.onrc.openvirtex.packet;

import java.nio.ByteBuffer;

/**
 * Base structures shared by L4 segments (refer to TCP, UDP).
 */
public abstract class AbstractSegment extends BasePacket {

    protected short sourcePort;
    protected short destinationPort;
    protected short checksum;

    /**
     * Gets the source port
     * 
     * @return the sourcePort
     */
    public short getSourcePort() {
        return this.sourcePort;
    }

    /**
     * Sets the source port
     * 
     * @param sourcePort
     *            the sourcePort to set
     */
    public AbstractSegment setSourcePort(final short sourcePort) {
        this.sourcePort = sourcePort;
        return this;
    }

    /**
     * Gets the destination port
     * 
     * @return the destinationPort
     */
    public short getDestinationPort() {
        return this.destinationPort;
    }

    /**
     * Sets the destination port
     * 
     * @param destinationPort
     *            the destinationPort to set
     */
    public AbstractSegment setDestinationPort(final short destinationPort) {
        this.destinationPort = destinationPort;
        return this;
    }

    /**
     * Gets the checksum
     * 
     * @return the checksum
     */
    public short getChecksum() {
        return this.checksum;
    }

    /**
     * Sets the checksum
     * 
     * @param checksum
     *            the checksum to set
     */
    public AbstractSegment setChecksum(final short checksum) {
        this.checksum = checksum;
        return this;
    }

    @Override
    public void resetChecksum() {
        this.checksum = 0;
        super.resetChecksum();
    }

    /**
     * Serialization method that calculates the checksum from pseudo- header.
     *
     * @param bb
     *            The ByteBuffer for the data being serialized
     * @param l4type
     *            The protocol type
     * @param length
     *            length of segment
     */
    public void serialize(final ByteBuffer bb, int length) {
        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;

            // compute pseudo header mac
            if (this.parent != null && this.parent instanceof IPv4) {
                final IPv4 ipv4 = (IPv4) this.parent;
                accumulation += (ipv4.getSourceAddress() >> 16 & 0xffff)
                        + (ipv4.getSourceAddress() & 0xffff);
                accumulation += (ipv4.getDestinationAddress() >> 16 & 0xffff)
                        + (ipv4.getDestinationAddress() & 0xffff);
                accumulation += ipv4.getProtocol() & 0xff;
                accumulation += length & 0xffff;
            }

            for (int i = 0; i < length / 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            // pad to an even number of shorts
            if (length % 2 > 0) {
                accumulation += (bb.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(6, this.checksum);
        }
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Method for subclass hashCode() to piggyback onto
     */
    public int hashCode(int prime) {
        int result = super.hashCode();
        result = prime * result + this.checksum;
        result = prime * result + this.destinationPort;
        result = prime * result + this.sourcePort;
        return result;
    }
}
