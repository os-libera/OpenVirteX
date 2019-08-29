/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */

/*******************************************************************************
 * Libera HyperVisor development based OpenVirteX for SDN 2.0
 *
 *   OpenFlow Version Up with OpenFlowj
 *
 * This is updated by Libera Project team in Korea University
 *
 * Author: Seong-Mun Kim (bebecry@gmail.com)
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import org.projectfloodlight.openflow.protocol.OFCapabilities;

import java.util.HashSet;
import java.util.Set;

public class OVXSwitchCapabilities {

    Set<OFCapabilities> ovx10;
    Set<OFCapabilities> ovx13;
    Set<OFCapabilities> dft;

    public OVXSwitchCapabilities() {

        ovx10 = new HashSet<OFCapabilities>();

        ovx10.add(OFCapabilities.FLOW_STATS);
        ovx10.add(OFCapabilities.TABLE_STATS);
        ovx10.add(OFCapabilities.PORT_STATS);
        ovx10.add(OFCapabilities.ARP_MATCH_IP);

        dft = new HashSet<OFCapabilities>();

        dft.add(OFCapabilities.FLOW_STATS);
        dft.add(OFCapabilities.TABLE_STATS);
        dft.add(OFCapabilities.PORT_STATS);
        dft.add(OFCapabilities.ARP_MATCH_IP);

        ovx13 = new HashSet<OFCapabilities>();

        ovx13.add(OFCapabilities.FLOW_STATS);
        ovx13.add(OFCapabilities.TABLE_STATS);
        ovx13.add(OFCapabilities.PORT_STATS);
        ovx13.add(OFCapabilities.IP_REASM);
    }

    public Set<OFCapabilities> getOVXSwitchCapabilitiesVer10() {
        return ovx10;
    }

    public Set<OFCapabilities> getOVXSwitchCapabilitiesVer13() {
        return ovx13;
    }

    public Set<OFCapabilities> getDefaultSwitchCapabilities() {
        return dft;
    }
}
