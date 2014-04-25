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
 *    Copyright 2012, Andrew Ferguson, Brown University
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

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;

/**
 * Represents an ofp_queue_get_config_request message
 *
 * @author Andrew Ferguson (adf@cs.brown.edu)
 */
public class OFQueueGetConfigReply extends OFMessage {
    public static int MINIMUM_LENGTH = 16;

    protected short portNumber;
    protected List<OFPacketQueue> queues = new ArrayList<OFPacketQueue>();

    public OFQueueGetConfigReply() {
        super();
        this.type = OFType.QUEUE_GET_CONFIG_REPLY;
        this.length = U16.t(OFQueueGetConfigReply.MINIMUM_LENGTH);
    }

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
     * @return the port's queues
     */
    public List<OFPacketQueue> getQueues() {
        return this.queues;
    }

    /**
     * @param queues
     *            the queues to set
     */
    public void setQueues(final List<OFPacketQueue> queues) {
        this.queues.clear();
        this.queues.addAll(queues);
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.portNumber = data.readShort();
        data.readInt(); // pad
        data.readShort(); // pad

        int availLength = this.length - OFQueueGetConfigReply.MINIMUM_LENGTH;
        this.queues.clear();

        while (availLength > 0) {
            final OFPacketQueue queue = new OFPacketQueue();
            queue.readFrom(data);
            this.queues.add(queue);
            availLength -= queue.getLength();
        }
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.portNumber);
        data.writeInt(0); // pad
        data.writeShort(0); // pad

        for (final OFPacketQueue queue : this.queues) {
            queue.writeTo(data);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 349;
        int result = super.hashCode();
        result = prime * result + this.portNumber;
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
        if (!(obj instanceof OFQueueGetConfigReply)) {
            return false;
        }
        final OFQueueGetConfigReply other = (OFQueueGetConfigReply) obj;
        if (this.portNumber != other.portNumber) {
            return false;
        }
        return true;
    }
}
