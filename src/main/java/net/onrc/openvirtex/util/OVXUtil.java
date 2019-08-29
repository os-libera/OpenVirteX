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
package net.onrc.openvirtex.util;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.exceptions.UnknownActionException;
import org.projectfloodlight.openflow.protocol.action.*;


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
                OFActionSetDlDst dldst = (OFActionSetDlDst) act;
                ret.put("type", "DL_DST");
                ret.put("dl_dst", dldst.getDlAddr().toString());
                break;
            case SET_DL_SRC:
                OFActionSetDlSrc dlsrc = (OFActionSetDlSrc) act;
                ret.put("type", "DL_SRC");
                ret.put("dl_src", dlsrc.getDlAddr().toString());
                break;
            case SET_NW_DST:
                OFActionSetNwDst nwdst = (OFActionSetNwDst) act;
                ret.put("type", "NW_DST");
                ret.put("nw_dst", nwdst.getNwAddr().toString());
                break;
            case SET_NW_SRC:
                OFActionSetNwSrc nwsrc = (OFActionSetNwSrc) act;
                ret.put("type", "NW_SRC");
                ret.put("nw_src", nwsrc.getNwAddr().toString());
                break;
            case SET_NW_TOS:

                OFActionSetNwTos nwtos = (OFActionSetNwTos) act;
                ret.put("type", "NW_TOS");
                ret.put("nw_tos", nwtos.getType().toString());
                break;
            case SET_TP_DST:
                OFActionSetTpDst tpdst = (OFActionSetTpDst) act;
                ret.put("type", "TP_DST");
                ret.put("tp_dst", tpdst.getTpPort());
                break;
            case SET_TP_SRC:
                OFActionSetTpSrc tpsrc = (OFActionSetTpSrc) act;
                ret.put("type", "TP_SRC");
                ret.put("tp_src", tpsrc.getTpPort());
                break;
            case SET_VLAN_VID:
                OFActionSetVlanVid vlan = (OFActionSetVlanVid) act;
                ret.put("type", "SET_VLAN");
                ret.put("vlan_id", vlan.getVlanVid().getVlan());
                break;
            case SET_VLAN_PCP:
                OFActionSetVlanPcp pcp = (OFActionSetVlanPcp) act;
                ret.put("type", "SET_VLAN_PCP");
                ret.put("vlan_pcp", pcp.getVlanPcp().getValue());
                break;
            case STRIP_VLAN:
                ret.put("type", "STRIP_VLAN");
                break;
            case SET_QUEUE:
                OFActionEnqueue enq = (OFActionEnqueue) act;
                ret.put("type", "ENQUEUE");
                ret.put("queue", enq.getQueueId());
                break;
            case SET_FIELD:
                OFActionSetField field = (OFActionSetField) act;
                ret.put("type", "SET_FIELD");
                break;
            case EXPERIMENTER:
//            case VENDOR:
                ret.put("type", "EXPERIMENTER");
                break;
            default:
                throw new UnknownActionException("Action " + act.getType()
                        + " is unknown.");

        }

        return ret;
    }

}
