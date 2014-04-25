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

/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Represents an ofp_action_vlan_vid
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionVirtualLanIdentifier extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected short virtualLanIdentifier;

    public OFActionVirtualLanIdentifier() {
        super.setType(OFActionType.SET_VLAN_ID);
        super.setLength((short) OFActionVirtualLanIdentifier.MINIMUM_LENGTH);
    }

    public OFActionVirtualLanIdentifier(final short vlanId) {
        this();
        this.virtualLanIdentifier = vlanId;
    }

    /**
     * @return the virtualLanIdentifier
     */
    public short getVirtualLanIdentifier() {
        return this.virtualLanIdentifier;
    }

    /**
     * @param virtualLanIdentifier
     *            the virtualLanIdentifier to set
     */
    public void setVirtualLanIdentifier(final short virtualLanIdentifier) {
        this.virtualLanIdentifier = virtualLanIdentifier;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.virtualLanIdentifier = data.readShort();
        data.readShort();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.virtualLanIdentifier);
        data.writeShort((short) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 383;
        int result = super.hashCode();
        result = prime * result + this.virtualLanIdentifier;
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
        if (!(obj instanceof OFActionVirtualLanIdentifier)) {
            return false;
        }
        final OFActionVirtualLanIdentifier other = (OFActionVirtualLanIdentifier) obj;
        if (this.virtualLanIdentifier != other.virtualLanIdentifier) {
            return false;
        }
        return true;
    }
}
