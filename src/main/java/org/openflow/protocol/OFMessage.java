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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;
import org.openflow.util.U32;
import org.openflow.util.U8;

/**
 * The base class for all OpenFlow protocol messages. This class contains the
 * equivalent of the ofp_header which is present in all OpenFlow messages.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Feb 3, 2010
 * @author Rob Sherwood (rob.sherwood@stanford.edu) - Feb 3, 2010
 */
public class OFMessage {
    public static byte OFP_VERSION = 0x01;
    public static int MINIMUM_LENGTH = 8;

    protected byte version;
    protected OFType type;
    protected short length;
    protected int xid;

    private ConcurrentHashMap<String, Object> storage;

    public OFMessage() {
        this.storage = null;
        this.version = OFMessage.OFP_VERSION;
    }

    protected synchronized ConcurrentHashMap<String, Object> getMessageStore() {
        if (this.storage == null) {
            this.storage = new ConcurrentHashMap<String, Object>();
            ;
        }
        return this.storage;
    }

    /**
     * Get the length of this message
     *
     * @return
     */
    public short getLength() {
        return this.length;
    }

    /**
     * Get the length of this message, unsigned
     *
     * @return
     */
    public int getLengthU() {
        return U16.f(this.length);
    }

    /**
     * Set the length of this message
     *
     * @param length
     */
    public OFMessage setLength(final short length) {
        this.length = length;
        return this;
    }

    /**
     * Set the length of this message, unsigned
     *
     * @param length
     */
    public OFMessage setLengthU(final int length) {
        this.length = U16.t(length);
        return this;
    }

    /**
     * Get the type of this message
     *
     * @return
     */
    public OFType getType() {
        return this.type;
    }

    /**
     * Set the type of this message
     *
     * @param type
     */
    public void setType(final OFType type) {
        this.type = type;
    }

    /**
     * Get the OpenFlow version of this message
     *
     * @return
     */
    public byte getVersion() {
        return this.version;
    }

    /**
     * Set the OpenFlow version of this message
     *
     * @param version
     */
    public void setVersion(final byte version) {
        this.version = version;
    }

    /**
     * Get the transaction id of this message
     *
     * @return
     */
    public int getXid() {
        return this.xid;
    }

    /**
     * Set the transaction id of this message
     *
     * @param xid
     */
    public void setXid(final int xid) {
        this.xid = xid;
    }

    /**
     * Read this message off the wire from the specified ByteBuffer
     *
     * @param data
     */
    public void readFrom(final ChannelBuffer data) {
        this.version = data.readByte();
        this.type = OFType.valueOf(data.readByte());
        this.length = data.readShort();
        this.xid = data.readInt();
    }

    /**
     * Write this message's binary format to the specified ByteBuffer
     *
     * @param data
     */
    public void writeTo(final ChannelBuffer data) {
        data.writeByte(this.version);
        data.writeByte(this.type.getTypeValue());
        data.writeShort(this.length);
        data.writeInt(this.xid);
    }

    /**
     * Returns a summary of the message
     *
     * @return "ofmsg=v=$version;t=$type:l=$len:xid=$xid"
     */
    @Override
    public String toString() {
        return "ofmsg" + ":v=" + U8.f(this.getVersion()) + ";t="
                + this.getType() + ";l=" + this.getLengthU() + ";x="
                + U32.f(this.getXid());
    }

    @Override
    public int hashCode() {
        final int prime = 97;
        int result = 1;
        result = prime * result + this.length;
        result = prime * result
                + (this.type == null ? 0 : this.type.hashCode());
        result = prime * result + this.version;
        result = prime * result + this.xid;
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
        if (!(obj instanceof OFMessage)) {
            return false;
        }
        final OFMessage other = (OFMessage) obj;
        if (this.length != other.length) {
            return false;
        }
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.xid != other.xid) {
            return false;
        }
        return true;
    }

    public static String getDataAsString(final OFMessage msg) {

        final StringBuffer sb = new StringBuffer("");

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        final Date date = new Date();

        sb.append(dateFormat.format(date));
        sb.append("      ");

        switch (msg.getType()) {
        case PACKET_IN:
            final OFPacketIn pktIn = (OFPacketIn) msg;

            sb.append("\ntotal length: ");
            sb.append(pktIn.getTotalLength());
            sb.append("\nin_port: ");
            sb.append(pktIn.getInPort());
            sb.append("\ndata_length: ");
            sb.append(pktIn.getTotalLength() - OFPacketIn.MINIMUM_LENGTH);
            sb.append("\nbuffer: ");
            sb.append(pktIn.getBufferId());

            break;

        case PACKET_OUT:
            final OFPacketOut pktOut = (OFPacketOut) msg;

            sb.append("\nin_port: ");
            sb.append(pktOut.getInPort());
            sb.append("\nactions_len: ");
            sb.append(pktOut.getActionsLength());
            if (pktOut.getActions() != null) {
                sb.append("\nactions: ");
                sb.append(pktOut.getActions().toString());
            }
            break;

        case FLOW_MOD:
            final OFFlowMod fm = (OFFlowMod) msg;

            sb.append("\nADD: cookie: ");
            sb.append(fm.getCookie());
            sb.append(" idle: ");
            sb.append(fm.getIdleTimeout());
            sb.append(" hard: ");
            sb.append(fm.getHardTimeout());
            sb.append(" pri: ");
            sb.append(fm.getPriority());
            sb.append(" buf: ");
            sb.append(fm.getBufferId());
            sb.append(" flg: ");
            sb.append(fm.getFlags());
            if (fm.getActions() != null) {
                sb.append("\nactions: ");
                sb.append(fm.getActions().toString());
            }
            break;

        default:
            sb.append("[Unknown Packet]");
        }

        sb.append("\n\n");
        return sb.toString();

    }

    public static byte[] getData(final OFMessage msg) {
        return OFMessage.getDataAsString(msg).getBytes();
    }
}
