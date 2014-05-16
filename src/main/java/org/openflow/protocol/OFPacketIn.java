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

package org.openflow.protocol;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;
import org.openflow.util.U32;
import org.openflow.util.U8;

/**
 * Represents an ofp_packet_in
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Feb 8, 2010
 */
public class OFPacketIn extends OFMessage {
    public static short MINIMUM_LENGTH = 18;

    public enum OFPacketInReason {
        NO_MATCH, ACTION
    }

    protected int bufferId;
    protected short totalLength;
    protected short inPort;
    protected OFPacketInReason reason;
    protected byte[] packetData;

    public OFPacketIn() {
        super();
        this.type = OFType.PACKET_IN;
        this.length = U16.t(OFPacketIn.MINIMUM_LENGTH);
    }

    /**
     * Get buffer_id
     *
     * @return
     */
    public int getBufferId() {
        return this.bufferId;
    }

    /**
     * Set buffer_id
     *
     * @param bufferId
     */
    public OFPacketIn setBufferId(final int bufferId) {
        this.bufferId = bufferId;
        return this;
    }

    /**
     * Returns the packet data
     *
     * @return
     */
    public byte[] getPacketData() {
        return this.packetData;
    }

    /**
     * Sets the packet data, and updates the length of this message
     *
     * @param packetData
     */
    public OFPacketIn setPacketData(final byte[] packetData) {
        this.packetData = packetData;
        this.length = U16.t(OFPacketIn.MINIMUM_LENGTH + packetData.length);
        return this;
    }

    /**
     * Get in_port
     *
     * @return
     */
    public short getInPort() {
        return this.inPort;
    }

    /**
     * Set in_port
     *
     * @param inPort
     */
    public OFPacketIn setInPort(final short inPort) {
        this.inPort = inPort;
        return this;
    }

    /**
     * Get reason
     *
     * @return
     */
    public OFPacketInReason getReason() {
        return this.reason;
    }

    /**
     * Set reason
     *
     * @param reason
     */
    public OFPacketIn setReason(final OFPacketInReason reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Get total_len
     *
     * @return
     */
    public short getTotalLength() {
        return this.totalLength;
    }

    /**
     * Set total_len
     *
     * @param totalLength
     */
    public OFPacketIn setTotalLength(final short totalLength) {
        this.totalLength = totalLength;
        return this;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.bufferId = data.readInt();
        this.totalLength = data.readShort();
        this.inPort = data.readShort();
        this.reason = OFPacketInReason.values()[U8.f(data.readByte())];
        data.readByte(); // pad
        this.packetData = new byte[this.getLengthU()
                - OFPacketIn.MINIMUM_LENGTH];
        data.readBytes(this.packetData);
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeInt(this.bufferId);
        data.writeShort(this.totalLength);
        data.writeShort(this.inPort);
        data.writeByte((byte) this.reason.ordinal());
        data.writeByte((byte) 0x0); // pad
        data.writeBytes(this.packetData);
    }

    @Override
    public int hashCode() {
        final int prime = 283;
        int result = super.hashCode();
        result = prime * result + this.bufferId;
        result = prime * result + this.inPort;
        result = prime * result + Arrays.hashCode(this.packetData);
        result = prime * result
                + (this.reason == null ? 0 : this.reason.hashCode());
        result = prime * result + this.totalLength;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFPacketIn)) {
            return false;
        }
        final OFPacketIn other = (OFPacketIn) obj;
        if (this.bufferId != other.bufferId) {
            return false;
        }
        if (this.inPort != other.inPort) {
            return false;
        }
        if (!Arrays.equals(this.packetData, other.packetData)) {
            return false;
        }
        if (this.reason == null) {
            if (other.reason != null) {
                return false;
            }
        } else if (!this.reason.equals(other.reason)) {
            return false;
        }
        if (this.totalLength != other.totalLength) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final String myStr = super.toString();
        return "packetIn" + ":bufferId=" + U32.f(this.bufferId) + myStr;
    }
}
