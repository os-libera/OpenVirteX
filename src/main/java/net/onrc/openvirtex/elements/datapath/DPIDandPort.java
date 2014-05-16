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
package net.onrc.openvirtex.elements.datapath;

public class DPIDandPort {
    long dpid;
    short port;

    public DPIDandPort(final long dpid, final short port) {
        super();
        this.dpid = dpid;
        this.port = port;
    }

    /**
     * @return the dpid
     */
    public long getDpid() {
        return this.dpid;
    }

    /**
     * @param dpid
     *            the dpid to set
     */
    public void setDpid(final long dpid) {
        this.dpid = dpid;
    }

    /**
     * @return the port
     */
    public short getPort() {
        return this.port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(final short port) {
        this.port = port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (dpid ^ (dpid >>> 32));
        result = prime * result + port;
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
        DPIDandPort other = (DPIDandPort) obj;
        if (dpid != other.dpid) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.dpid + ":" + this.port;
    }
}
