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
package net.onrc.openvirtex.elements.network;

import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 *
 */
public class Uplink {

    private PhysicalPort uplinkPort;

    private IPAddress uplinkIp;

    private MACAddress uplinkMac;

    private IPAddress nextHopIp;

    private MACAddress nectHopMac;

    public PhysicalPort getUplinkPort() {
        return this.uplinkPort;
    }

    public void setUplinkPort(final PhysicalPort uplinkPort) {
        this.uplinkPort = uplinkPort;
    }

    public IPAddress getUplinkIp() {
        return this.uplinkIp;
    }

    public void setUplinkIp(final IPAddress uplinkIp) {
        this.uplinkIp = uplinkIp;
    }

    public MACAddress getUplinkMac() {
        return this.uplinkMac;
    }

    public void setUplinkMac(final MACAddress uplinkMac) {
        this.uplinkMac = uplinkMac;
    }

    public IPAddress getNextHopIp() {
        return this.nextHopIp;
    }

    public void setNextHopIp(final IPAddress nextHopIp) {
        this.nextHopIp = nextHopIp;
    }

    public MACAddress getNectHopMac() {
        return this.nectHopMac;
    }

    public void setNectHopMac(final MACAddress nectHopMac) {
        this.nectHopMac = nectHopMac;
    }

}
