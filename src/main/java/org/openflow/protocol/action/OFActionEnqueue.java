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
public class OFActionEnqueue extends OFAction {
    public static int MINIMUM_LENGTH = 16;

    protected short port;
    protected int queueId;

    public OFActionEnqueue() {
        super.setType(OFActionType.OPAQUE_ENQUEUE);
        super.setLength((short) OFActionEnqueue.MINIMUM_LENGTH);
    }

    public OFActionEnqueue(final short port, final int queueId) {
        this();
        this.port = port;
        this.queueId = queueId;
    }

    /**
     * Get the output port
     *
     * @return
     */
    public short getPort() {
        return this.port;
    }

    /**
     * Set the output port
     *
     * @param port
     */
    public void setPort(final short port) {
        this.port = port;
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

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.port = data.readShort();
        data.readShort();
        data.readInt();
        this.queueId = data.readInt();
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.port);
        data.writeShort((short) 0);
        data.writeInt(0);
        data.writeInt(this.queueId);
    }

    @Override
    public int hashCode() {
        final int prime = 349;
        int result = super.hashCode();
        result = prime * result + this.port;
        result = prime * result + this.queueId;
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
        if (!(obj instanceof OFActionEnqueue)) {
            return false;
        }
        final OFActionEnqueue other = (OFActionEnqueue) obj;
        if (this.port != other.port) {
            return false;
        }
        if (this.queueId != other.queueId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        builder.append("[");
        builder.append("Port: ");
        builder.append(this.port);
        builder.append(", Queue Id: ");
        builder.append(this.queueId);
        builder.append("]");
        return builder.toString();
    }
}
