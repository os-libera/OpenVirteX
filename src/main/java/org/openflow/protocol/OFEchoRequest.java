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

/**
 * Represents an ofp_echo_request message
 *
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 */

public class OFEchoRequest extends OFMessage {
    public static int MINIMUM_LENGTH = 8;
    byte[] payload;

    public OFEchoRequest() {
        super();
        this.type = OFType.ECHO_REQUEST;
        this.length = U16.t(OFEchoRequest.MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(final ChannelBuffer bb) {
        super.readFrom(bb);
        final int datalen = this.getLengthU() - OFEchoRequest.MINIMUM_LENGTH;
        if (datalen > 0) {
            this.payload = new byte[datalen];
            bb.readBytes(this.payload);
        }
    }

    /**
     * @return the payload
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * @param payload
     *            the payload to set
     */
    public void setPayload(final byte[] payload) {
        this.payload = payload;
    }

    @Override
    public void writeTo(final ChannelBuffer bb) {
        super.writeTo(bb);
        if (this.payload != null) {
            bb.writeBytes(this.payload);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(this.payload);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final OFEchoRequest other = (OFEchoRequest) obj;
        if (!Arrays.equals(this.payload, other.payload)) {
            return false;
        }
        return true;
    }
}
