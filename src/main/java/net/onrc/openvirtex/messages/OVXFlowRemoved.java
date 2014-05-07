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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.exceptions.MappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

public class OVXFlowRemoved extends OFFlowRemoved implements Virtualizable {

    Logger log = LogManager.getLogger(OVXFlowRemoved.class.getName());

    @Override
    public void virtualize(final PhysicalSwitch sw) {

        int tid = (int) (this.cookie >> 32);

        /* a PhysSwitch can be a OVXLink */
        if (!(sw.getMap().hasVirtualSwitch(sw, tid))) {
            return;
        }
        try {
            OVXSwitch vsw = sw.getMap().getVirtualSwitch(sw, tid);
            /*
             * If we are a Big Switch we might receive multiple same-cookie FR's
             * from multiple PhysicalSwitches. Only handle if the FR's newly
             * seen
             */
            if (vsw.getFlowTable().hasFlowMod(this.cookie)) {
                OVXFlowMod fm = vsw.getFlowMod(this.cookie);
                /*
                 * send north ONLY if tenant controller wanted a FlowRemoved for
                 * the FlowMod
                 */
                vsw.deleteFlowMod(this.cookie);
                if (fm.hasFlag(OFFlowMod.OFPFF_SEND_FLOW_REM)) {
                    writeFields(fm);
                    vsw.sendMsg(this, sw);
                }
            }
        } catch (MappingException e) {
            log.warn("Exception fetching FlowMod from FlowTable: {}", e);
        }
    }

    /**
     * rewrites the fields of this message using values from the supplied
     * FlowMod.
     *
     * @param fm
     *            the original FlowMod associated with this FlowRemoved
     * @return the physical cookie
     */
    private void writeFields(OVXFlowMod fm) {
        this.cookie = fm.getCookie();
        this.match = fm.getMatch();
        this.priority = fm.getPriority();
        this.idleTimeout = fm.getIdleTimeout();
    }

    @Override
    public String toString() {
        return "OVXFlowRemoved: cookie=" + this.cookie + " priority="
                + this.priority + " match=" + this.match + " reason="
                + this.reason;
    }

}
