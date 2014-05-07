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
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFPortMod;

public class OVXPortMod extends OFPortMod implements Devirtualizable {

    @Override
    public void devirtualize(final OVXSwitch sw) {
        // TODO Auto-generated method stub
        // assume port numbers are virtual
        final OVXPort p = sw.getPort(this.getPortNumber());
        if (p == null) {
            sw.sendMsg(OVXMessageUtil.makeErrorMsg(
                    OFBadRequestCode.OFPBRC_EPERM, this), sw);
            return;
        }
        // set physical port number - anything else to do?
        final PhysicalPort phyPort = p.getPhysicalPort();
        this.setPortNumber(phyPort.getPortNumber());

        OVXMessageUtil.translateXid(this, p);
    }

}
