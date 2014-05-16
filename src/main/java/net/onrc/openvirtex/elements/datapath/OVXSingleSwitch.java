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

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

public class OVXSingleSwitch extends OVXSwitch {

    private static Logger log = LogManager.getLogger(OVXSingleSwitch.class
            .getName());

    public OVXSingleSwitch(final long switchId, final int tenantId) {
        super(switchId, tenantId);
    }

    @Override
    public boolean removePort(final Short portNumber) {
        if (!this.portMap.containsKey(portNumber)) {
            return false;
        } else {
            // TODO: this should generate a portstatus message to the ctrl
            this.portMap.remove(portNumber);
            return true;
        }
    }

    @Override
    // TODO: this is probably not optimal
    public void sendSouth(final OFMessage msg, final OVXPort inPort) {
        PhysicalSwitch psw = getPhySwitch(inPort);
        log.debug("Sending packet to sw {}: {}", psw.getName(), msg);
        psw.sendMsg(msg, this);
    }

    @Override
    public int translate(final OFMessage ofm, final OVXPort inPort) {
        // get new xid from only PhysicalSwitch tied to this switch
        PhysicalSwitch psw = getPhySwitch(inPort);
        return psw.translate(ofm, this);
    }


    private PhysicalSwitch getPhySwitch(OVXPort inPort) {
        PhysicalSwitch psw = null;
        if (inPort == null) {
            try {
                psw = this.map.getPhysicalSwitches(this).get(0);
            } catch (SwitchMappingException e) {
                log.warn("Cannot recover physical switch : {}", e);
            }
        } else {
            return inPort.getPhysicalPort().getParentSwitch();
        }
        return psw;
    }
}
