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
 * Represents an ofp_action_enqueue
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionNetworkTypeOfService extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected byte networkTypeOfService;

    public OFActionNetworkTypeOfService() {
        super.setType(OFActionType.SET_NW_TOS);
        super.setLength((short) OFActionNetworkTypeOfService.MINIMUM_LENGTH);
    }

    public OFActionNetworkTypeOfService(final byte tos) {
        this();
        this.networkTypeOfService = tos;
    }

    /**
     * @return the networkTypeOfService
     */
    public byte getNetworkTypeOfService() {
        return this.networkTypeOfService;
    }

    /**
     * @param networkTypeOfService
     *            the networkTypeOfService to set
     */
    public void setNetworkTypeOfService(final byte networkTypeOfService) {
        this.networkTypeOfService = networkTypeOfService;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.networkTypeOfService = data.readByte();
        data.readShort();
        data.readByte();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeByte(this.networkTypeOfService);
        data.writeShort((short) 0);
        data.writeByte((byte) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 359;
        int result = super.hashCode();
        result = prime * result + this.networkTypeOfService;
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
        if (!(obj instanceof OFActionNetworkTypeOfService)) {
            return false;
        }
        final OFActionNetworkTypeOfService other = (OFActionNetworkTypeOfService) obj;
        if (this.networkTypeOfService != other.networkTypeOfService) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        builder.append("[");
        builder.append(this.networkTypeOfService);
        builder.append("]");
        return builder.toString();
    }
}
