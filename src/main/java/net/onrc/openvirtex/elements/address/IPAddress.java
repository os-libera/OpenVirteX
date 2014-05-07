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
 *
 */
package net.onrc.openvirtex.elements.address;

import org.openflow.util.U8;

import net.onrc.openvirtex.packet.IPv4;

public abstract class IPAddress {
    protected int ip;

    protected IPAddress(final String ipAddress) {
        this.ip = IPv4.toIPv4Address(ipAddress);
    }

    protected IPAddress() {
    }

    public int getIp() {
        return this.ip;
    }

    public void setIp(final int ip) {
        this.ip = ip;
    }

    public String toSimpleString() {
        return U8.f((byte) (this.ip >> 24)) + "." + (this.ip >> 16 & 0xFF)
                + "." + (this.ip >> 8 & 0xFF) + "." + (this.ip & 0xFF);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + (this.ip >> 24) + "."
                + (this.ip >> 16 & 0xFF) + "." + (this.ip >> 8 & 0xFF) + "."
                + (this.ip & 0xFF) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ip;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IPAddress other = (IPAddress) obj;
        if (ip != other.ip) {
            return false;
        }
        return true;
    }
}
