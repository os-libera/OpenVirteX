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
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.factory.OFActionFactory;
import org.openflow.protocol.factory.OFActionFactoryAware;
import org.openflow.util.HexString;
import org.openflow.util.U16;

/**
 * Represents an ofp_packet_out message
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 12, 2010
 */
public class OFPacketOut extends OFMessage implements OFActionFactoryAware {
    public static int MINIMUM_LENGTH = 16;
    public static int BUFFER_ID_NONE = 0xffffffff;

    protected OFActionFactory actionFactory;
    protected int bufferId;
    protected short inPort;
    protected short actionsLength;
    protected List<OFAction> actions;
    protected byte[] packetData;

    public OFPacketOut() {
        super();
        this.type = OFType.PACKET_OUT;
        this.length = U16.t(OFPacketOut.MINIMUM_LENGTH);
        this.bufferId = OFPacketOut.BUFFER_ID_NONE;
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
    public OFPacketOut setBufferId(final int bufferId) {
        if (this.packetData != null && this.packetData.length > 0
                && bufferId != OFPacketOut.BUFFER_ID_NONE) {
            throw new IllegalArgumentException(
                    "PacketOut should not have both bufferId and packetData set");
        }
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
     * Sets the packet data
     *
     * @param packetData
     */
    public OFPacketOut setPacketData(final byte[] packetData) {
        if (packetData != null && packetData.length > 0
                && this.bufferId != OFPacketOut.BUFFER_ID_NONE) {
            throw new IllegalArgumentException(
                    "PacketOut should not have both bufferId and packetData set");
        }
        this.packetData = packetData;
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
    public OFPacketOut setInPort(final short inPort) {
        this.inPort = inPort;
        return this;
    }

    /**
     * Set in_port. Convenience method using OFPort enum.
     *
     * @param inPort
     */
    public OFPacketOut setInPort(final OFPort inPort) {
        this.inPort = inPort.getValue();
        return this;
    }

    /**
     * Get actions_len
     *
     * @return
     */
    public short getActionsLength() {
        return this.actionsLength;
    }

    /**
     * Get actions_len, unsigned
     *
     * @return
     */
    public int getActionsLengthU() {
        return U16.f(this.actionsLength);
    }

    /**
     * Set actions_len
     *
     * @param actionsLength
     */
    public OFPacketOut setActionsLength(final short actionsLength) {
        this.actionsLength = actionsLength;
        return this;
    }

    /**
     * Returns the actions contained in this message
     *
     * @return a list of ordered OFAction objects
     */
    public List<OFAction> getActions() {
        return this.actions;
    }

    /**
     * Sets the list of actions on this message
     *
     * @param actions
     *            a list of ordered OFAction objects
     */
    public OFPacketOut setActions(final List<OFAction> actions) {
        this.actions = actions;
        return this;
    }

    @Override
    public void setActionFactory(final OFActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.bufferId = data.readInt();
        this.inPort = data.readShort();
        this.actionsLength = data.readShort();
        if (this.actionFactory == null) {
            throw new RuntimeException("ActionFactory not set");
        }
        this.actions = this.actionFactory.parseActions(data,
                this.getActionsLengthU());
        this.packetData = new byte[this.getLengthU()
                - OFPacketOut.MINIMUM_LENGTH - this.getActionsLengthU()];
        data.readBytes(this.packetData);
        this.validate();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        this.validate();
        super.writeTo(data);
        data.writeInt(this.bufferId);
        data.writeShort(this.inPort);
        data.writeShort(this.actionsLength);
        for (final OFAction action : this.actions) {
            action.writeTo(data);
        }
        if (this.packetData != null) {
            data.writeBytes(this.packetData);
        }
    }

    /** validate the invariants of this OFMessage hold */
    public void validate() {
        if (!(this.bufferId != OFPacketOut.BUFFER_ID_NONE ^ (this.packetData != null && this.packetData.length > 0))) {
            throw new IllegalStateException(
                    "OFPacketOut must have exactly one of (bufferId, packetData) set (not one, not both)");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 293;
        int result = super.hashCode();
        result = prime * result
                + (this.actions == null ? 0 : this.actions.hashCode());
        result = prime * result + this.actionsLength;
        result = prime * result + this.bufferId;
        result = prime * result + this.inPort;
        result = prime * result + Arrays.hashCode(this.packetData);
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
        if (!(obj instanceof OFPacketOut)) {
            return false;
        }
        final OFPacketOut other = (OFPacketOut) obj;
        if (this.actions == null) {
            if (other.actions != null) {
                return false;
            }
        } else if (!this.actions.equals(other.actions)) {
            return false;
        }
        if (this.actionsLength != other.actionsLength) {
            return false;
        }
        if (this.bufferId != other.bufferId) {
            return false;
        }
        if (this.inPort != other.inPort) {
            return false;
        }
        if (!Arrays.equals(this.packetData, other.packetData)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFPacketOut [actionFactory=" + this.actionFactory
                + ", actions=" + this.actions + ", actionsLength="
                + this.actionsLength + ", bufferId=0x"
                + Integer.toHexString(this.bufferId) + ", inPort="
                + this.inPort + ", packetData="
                + HexString.toHexString(this.packetData) + "]";
    }
}
