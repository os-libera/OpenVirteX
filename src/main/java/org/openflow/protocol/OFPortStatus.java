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

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;

/**
 * Represents an ofp_port_status message
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFPortStatus extends OFMessage {
    public static int MINIMUM_LENGTH = 64;

    public enum OFPortReason {
        OFPPR_ADD((byte) 0), OFPPR_DELETE((byte) 1), OFPPR_MODIFY((byte) 2);

        private byte reason;

        private OFPortReason(final byte reason) {
            this.reason = reason;
        }

        public byte getReasonCode() {
            return this.reason;
        }

        public static OFPortReason fromReasonCode(final byte reason) {
            for (final OFPortReason r : OFPortReason.values()) {
                if (r.getReasonCode() == reason) {
                    return r;
                }
            }
            return null;
        }
    }

    protected byte reason;
    protected OFPhysicalPort desc;

    /**
     * @return the reason
     */
    public byte getReason() {
        return this.reason;
    }

    /**
     * @param reason
     *            the reason to set
     */
    public void setReason(final byte reason) {
        this.reason = reason;
    }

    /**
     * @return the desc
     */
    public OFPhysicalPort getDesc() {
        return this.desc;
    }

    /**
     * @param desc
     *            the desc to set
     */
    public void setDesc(final OFPhysicalPort desc) {
        this.desc = desc;
    }

    public OFPortStatus() {
        super();
        this.type = OFType.PORT_STATUS;
        this.length = U16.t(OFPortStatus.MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.reason = data.readByte();
        data.readerIndex(data.readerIndex() + 7); // skip 7 bytes of padding
        if (this.desc == null) {
            this.desc = new OFPhysicalPort();
        }
        this.desc.readFrom(data);
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeByte(this.reason);
        for (int i = 0; i < 7; ++i) {
            data.writeByte((byte) 0);
        }
        this.desc.writeTo(data);
    }

    @Override
    public int hashCode() {
        final int prime = 313;
        int result = super.hashCode();
        result = prime * result
                + (this.desc == null ? 0 : this.desc.hashCode());
        result = prime * result + this.reason;
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
        if (!(obj instanceof OFPortStatus)) {
            return false;
        }
        final OFPortStatus other = (OFPortStatus) obj;
        if (this.desc == null) {
            if (other.desc != null) {
                return false;
            }
        } else if (!this.desc.equals(other.desc)) {
            return false;
        }
        if (this.reason != other.reason) {
            return false;
        }
        return true;
    }
}
