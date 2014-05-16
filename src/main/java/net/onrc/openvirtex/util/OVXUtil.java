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
package net.onrc.openvirtex.util;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.exceptions.UnknownActionException;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;

/**
 * OVX utility class that implements various methods.
 */
public final class OVXUtil {

    /**
     * Override default constructor with no-op private constructor.
     * Needed for checkstyle.
     */
    private OVXUtil() {
    }

    /**
     * Gets the minimum number of bits needed to represent the given
     * integer.
     *
     * @param x the integer to represent in binary
     * @return the number of bits
     */
    public static int numBitsneeded(int x) {
        int counter = 0;
        while (x != 0) {
            x >>= 1;
            counter++;
        }
        return counter;
    }

    /**
     * Gets a map with string keys and object values from
     * the given action.
     *
     * @param act the action
     * @return string-to-object map
     * @throws UnknownActionException
     */
    public static Map<String, Object> actionToMap(OFAction act)
            throws UnknownActionException {
        HashMap<String, Object> ret = new HashMap<String, Object>();

        switch (act.getType()) {
        case OUTPUT:
            OFActionOutput out = (OFActionOutput) act;
            ret.put("type", "OUTPUT");
            ret.put("port", out.getPort());
            break;
        case SET_DL_DST:
            OFActionDataLayerDestination dldst = (OFActionDataLayerDestination) act;
            ret.put("type", "DL_DST");
            ret.put("dl_dst",
                    new MACAddress(dldst.getDataLayerAddress()).toString());
            break;
        case SET_DL_SRC:
            OFActionDataLayerSource dlsrc = (OFActionDataLayerSource) act;
            ret.put("type", "DL_SRC");
            ret.put("dl_src",
                    new MACAddress(dlsrc.getDataLayerAddress()).toString());
            break;
        case SET_NW_DST:
            OFActionNetworkLayerDestination nwdst = (OFActionNetworkLayerDestination) act;
            ret.put("type", "NW_DST");
            ret.put("nw_dst", new PhysicalIPAddress(nwdst.getNetworkAddress())
                    .toSimpleString());
            break;
        case SET_NW_SRC:
            OFActionNetworkLayerSource nwsrc = (OFActionNetworkLayerSource) act;
            ret.put("type", "NW_SRC");
            ret.put("nw_src", new PhysicalIPAddress(nwsrc.getNetworkAddress())
                    .toSimpleString());
            break;
        case SET_NW_TOS:
            OFActionNetworkTypeOfService nwtos = (OFActionNetworkTypeOfService) act;
            ret.put("type", "NW_TOS");
            ret.put("nw_tos", nwtos.getNetworkTypeOfService());
            break;
        case SET_TP_DST:
            OFActionTransportLayerDestination tpdst = (OFActionTransportLayerDestination) act;
            ret.put("type", "TP_DST");
            ret.put("tp_dst", tpdst.getTransportPort());
            break;
        case SET_TP_SRC:
            OFActionTransportLayerSource tpsrc = (OFActionTransportLayerSource) act;
            ret.put("type", "TP_SRC");
            ret.put("tp_src", tpsrc.getTransportPort());
            break;
        case SET_VLAN_ID:
            OFActionVirtualLanIdentifier vlan = (OFActionVirtualLanIdentifier) act;
            ret.put("type", "SET_VLAN");
            ret.put("vlan_id", vlan.getVirtualLanIdentifier());
            break;
        case SET_VLAN_PCP:
            OFActionVirtualLanPriorityCodePoint pcp = (OFActionVirtualLanPriorityCodePoint) act;
            ret.put("type", "SET_VLAN_PCP");
            ret.put("vlan_pcp", pcp.getVirtualLanPriorityCodePoint());
            break;
        case STRIP_VLAN:
            ret.put("type", "STRIP_VLAN");
            break;
        case OPAQUE_ENQUEUE:
            OFActionEnqueue enq = (OFActionEnqueue) act;
            ret.put("type", "ENQUEUE");
            ret.put("queue", enq.getQueueId());
            break;
        case VENDOR:
            ret.put("type", "VENDOR");
            break;
        default:
            throw new UnknownActionException("Action " + act.getType()
                    + " is unknown.");

        }

        return ret;
    }

}
