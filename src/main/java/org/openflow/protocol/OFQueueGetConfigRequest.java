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

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;

/**
 * Represents an ofp_queue_get_config_request message
 *
 * @author Andrew Ferguson (adf@cs.brown.edu)
 */
public class OFQueueGetConfigRequest extends OFMessage {
    public static int MINIMUM_LENGTH = 12;

    protected short portNumber;

    public OFQueueGetConfigRequest(final short portNumber) {
        super();
        this.type = OFType.QUEUE_GET_CONFIG_REQUEST;
        this.length = U16.t(OFQueueGetConfigRequest.MINIMUM_LENGTH);
        this.portNumber = portNumber;
    }

    public OFQueueGetConfigRequest() {
        this((short) 0);
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

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.portNumber = data.readShort();
        data.readShort(); // pad
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.portNumber);
        data.writeShort(0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 347;
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
        if (!(obj instanceof OFQueueGetConfigRequest)) {
            return false;
        }
        final OFQueueGetConfigRequest other = (OFQueueGetConfigRequest) obj;
        if (this.portNumber != other.portNumber) {
            return false;
        }
        return true;
    }
}
