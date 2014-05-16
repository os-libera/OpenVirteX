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

/**
 * Base class representing ofp_switch_config based messages
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public abstract class OFSwitchConfig extends OFMessage {
    public static int MINIMUM_LENGTH = 12;

    public enum OFConfigFlags {
        OFPC_FRAG_NORMAL, OFPC_FRAG_DROP, OFPC_FRAG_REASM, OFPC_FRAG_MASK
    }

    protected short flags;
    protected short missSendLength;

    public OFSwitchConfig() {
        super();
        super.setLengthU(OFSwitchConfig.MINIMUM_LENGTH);
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return this.flags;
    }

    /**
     * @param flags
     *            the flags to set
     */
    public OFSwitchConfig setFlags(final short flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return the missSendLength
     */
    public short getMissSendLength() {
        return this.missSendLength;
    }

    /**
     * @param missSendLength
     *            the missSendLength to set
     */
    public OFSwitchConfig setMissSendLength(final short missSendLength) {
        this.missSendLength = missSendLength;
        return this;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.flags = data.readShort();
        this.missSendLength = data.readShort();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.flags);
        data.writeShort(this.missSendLength);
    }

    @Override
    public int hashCode() {
        final int prime = 331;
        int result = super.hashCode();
        result = prime * result + this.flags;
        result = prime * result + this.missSendLength;
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
        if (!(obj instanceof OFSwitchConfig)) {
            return false;
        }
        final OFSwitchConfig other = (OFSwitchConfig) obj;
        if (this.flags != other.flags) {
            return false;
        }
        if (this.missSendLength != other.missSendLength) {
            return false;
        }
        return true;
    }
}
