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

import java.util.Map;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.LinkPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.util.OVXStateManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus;

/**
 * Handler for PortStatus messages from the network. From the virtual side, HOW
 * the port was disabled in the network doesn't matter, so only a PhysicalPort
 * is duly affected by state/configuration changes in the message.
 */
public class OVXPortStatus extends OFPortStatus implements Virtualizable {

    private final Logger log = LogManager.getLogger(OVXPortStatus.class);

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        log.debug("Received {} from switch {} for port {}", this.toString(), sw
                .getSwitchId(),
                sw.getPort(this.desc.getPortNumber()) == null ? "null" : sw
                        .getPort(this.desc.getPortNumber()).toAP());

        PhysicalPort p = sw.getPort(this.desc.getPortNumber());
        if (p == null) {
            handlePortAdd(sw);
            return;
        }

        LinkPair<PhysicalLink> plink = p.getLink();
        OVXStateManager mgr = new OVXStateManager();

        if (this.isReason(OFPortReason.OFPPR_MODIFY)) {
            if (isState(OFPortState.OFPPS_LINK_DOWN)) {
                /* Link went down or port was disabled */
                mgr.deactivateOVXPorts(p, false);
                if ((plink != null) && (plink.exists())) {
                    mgr.deactivateVLinks(plink.getOutLink(), false);
                    mgr.deactivateVLinks(plink.getInLink(), false);
                }
                p.setConfig(this.getDesc().getConfig());
                p.tearDown();
            } else if (!isState(OFPortState.OFPPS_LINK_DOWN)) {
                /* Link is back up or port was enabled */
                p.setConfig(this.getDesc().getConfig());
                p.boot();
                mgr.activateOVXPorts(p);
                if ((plink != null) && (plink.exists())) {
                    mgr.activateVLinks(plink.getOutLink());
                    mgr.activateVLinks(plink.getInLink());
                }
            } else {
                /* Some port attribute has changed */
                this.updateOVXPorts(p);
            }
        } else if (this.isReason(OFPortReason.OFPPR_DELETE)) {
            mgr.deactivateOVXPorts(p, true);
            if ((plink != null) && (plink.exists())) {
                mgr.deactivateVLinks(plink.getOutLink(), true);
                mgr.deactivateVLinks(plink.getInLink(), true);
            }
            p.tearDown();
            p.unregister();
        } else {
            log.warn("Unknown PortReason [code={}], ignoring", this.reason);
        }
    }

    private void handlePortAdd(PhysicalSwitch sw) {
        /* add a new port to PhySwitch if add message, quit otherwise */
        if (isReason(OFPortReason.OFPPR_ADD)) {
            PhysicalPort p = new PhysicalPort(this.desc, sw, true);
            p.register();
        }
    }

    /**
     * Update state/config of OVXPorts based on changes in PhysicalPort
     * according to this PortStatus. This would be in response to non-
     * OFPPS_LINK_DOWN OFPPR_MODIFY messages.
     * 
     * @param ppt
     *            the PhysicalPort
     */
    private void updateOVXPorts(PhysicalPort ppt) {
        for (Map<Integer, OVXPort> el : ppt.getOVXPorts(null)) {
            for (OVXPort vp : el.values()) {
                vp.applyPortStatus(this);
            }
        }
    }

    public boolean isReason(OFPortReason reason) {
        return this.reason == reason.getReasonCode();
    }

    public boolean isState(OFPortState state) {
        return this.desc.getState() == state.getValue();
    }

    @Override
    public String toString() {
        return "OVXPortStatus: reason["
                + OFPortReason.fromReasonCode(this.reason).name() + "]"
                + " port[" + this.desc.getPortNumber() + "]";
    }

}
