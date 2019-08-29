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
package net.onrc.openvirtex.messages.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.action.OFAction;

public final class OVXActionUtil {

    private static Logger log = LogManager.getLogger(OVXActionUtil.class.getName());


    private OVXActionUtil() {}

    public static OVXAction wrappingOVXAction(OFAction ofAction) {
        switch(ofAction.getType()) {
/*            case COPY_TTL_IN:
                break;
            case COPY_TTL_OUT:
                break;
            case DEC_MPLS_TTL:
                break;
            case DEC_NW_TTL:
                break;
            case EXPERIMENTER:
                break;
            case GROUP:
                break;
            case POP_MPLS:
                break;
            case POP_PBB:
                break;
            case POP_VLAN:
                break;
            case PUSH_MPLS:
                break;
            case PUSH_PBB:
                break;
            case PUSH_VLAN:
                break;
            case SET_NW_ECN:
                break;
            case SET_FIELD:
                break;
            case SET_MPLS_LABEL:
                break;
            case SET_MPLS_TC:
                break;
            case SET_MPLS_TTL:
                break;
            case SET_NW_TTL:
                break;
                */
            case OUTPUT:
                return new OVXActionOutput(ofAction);
            case SET_DL_DST:
                return new OVXActionSetNwDst(ofAction);
            case SET_DL_SRC:
                return new OVXActionSetNwSrc(ofAction);
            case SET_NW_DST:
                return new OVXActionSetNwDst(ofAction);
            case SET_NW_SRC:
                return new OVXActionSetNwSrc(ofAction);
            case SET_NW_TOS:
                return new OVXActionSetNwTos(ofAction);
            case SET_QUEUE:
                return new OVXActionSetEnqueue(ofAction);
            case SET_TP_DST:
                return new OVXActionSetTpDst(ofAction);
            case SET_TP_SRC:
                return new OVXActionSetTpSrc(ofAction);
            case SET_VLAN_PCP:
                return new OVXActionSetVlanPcp(ofAction);
            case SET_VLAN_VID:
                return new OVXActionSetVlanVid(ofAction);
            case STRIP_VLAN:
                return new OVXActionStripVlan(ofAction);
            //for  OF_1.3
            case SET_FIELD:
                return new OVXActionSetField(ofAction);
            default:
                log.info("Unsupported Action " + ofAction.toString());
                return null;
        }
    }
}
