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
package net.onrc.openvirtex.elements.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.messages.OVXPortStatus;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

/**
 * A physical port maintains the mapping of all virtual ports that are mapped to
 * it.
 */
public class PhysicalPort extends Port<PhysicalSwitch, PhysicalLink> {


    private final Map<Integer, HashMap<Integer, OVXPort>> ovxPortMap;

    /**
     * Instantiates a physical port based on an OpenFlow physical port.
     *
     * @param port
     *            the OpenFlow physical port
     */
    private PhysicalPort(final OFPhysicalPort port) {
        super(port);
        this.ovxPortMap = new HashMap<Integer, HashMap<Integer, OVXPort>>();
    }

    /**
     * Instantiates a physical port based on an OpenFlow physical port, the
     * physical switch it belongs to, and and set whether the port is an edge
     * port or not.
     *
     * @param port
     *            the OpenFlow physical port
     * @param sw
     *            the physical switch
     * @param isEdge
     *            edge attribute
     */
    public PhysicalPort(final OFPhysicalPort port, final PhysicalSwitch sw,
            final boolean isEdge) {
        this(port);
        this.parentSwitch = sw;
        this.isEdge = isEdge;
    }

    /**
     * Gets the virtual port that maps this physical port for the given tenant
     * ID and virtual link ID.
     *
     * @param tenantId
     *            the virtual network ID
     * @param vLinkId
     *            the virtual link ID
     *
     * @return the virtual port instance, null if the tenant ID or the virtual
     *         link ID are invalid
     */
    public OVXPort getOVXPort(final Integer tenantId, final Integer vLinkId) {
        if (this.ovxPortMap.get(tenantId) == null) {
            return null;
        }
        OVXPort p = this.ovxPortMap.get(tenantId).get(vLinkId);
        if (p != null && !p.isActive()) {
            return null;
        }
        return p;
    }

    /**
     * Maps the given virtual port to this physical port.
     *
     * @param ovxPort
     *            the virtual port
     */
    public void setOVXPort(final OVXPort ovxPort) {
        if (this.ovxPortMap.get(ovxPort.getTenantId()) != null) {
            if (ovxPort.getLink() != null) {
                this.ovxPortMap.get(ovxPort.getTenantId()).put(
                        ovxPort.getLink().getInLink().getLinkId(), ovxPort);
            } else {
                this.ovxPortMap.get(ovxPort.getTenantId()).put(0, ovxPort);
            }
        } else {
            final HashMap<Integer, OVXPort> portMap = new HashMap<Integer, OVXPort>();
            if (ovxPort.getLink() != null) {
                portMap.put(ovxPort.getLink().getOutLink().getLinkId(), ovxPort);
            } else {
                portMap.put(0, ovxPort);
            }
            this.ovxPortMap.put(ovxPort.getTenantId(), portMap);
        }
    }

    @Override
    public Map<String, Object> getDBIndex() {
        return null;
    }

    @Override
    public String getDBKey() {
        return null;
    }

    @Override
    public String getDBName() {
        return DBManager.DB_VNET;
    }

    @Override
    public Map<String, Object> getDBObject() {
        Map<String, Object> dbObject = new HashMap<String, Object>();
        dbObject.put(TenantHandler.DPID, this.getParentSwitch().getSwitchId());
        dbObject.put(TenantHandler.PORT, this.portNumber);
        return dbObject;
    }

    /**
     * Removes mapping between given virtual port and this physical port.
     *
     * @param ovxPort
     *            the virtual port
     */
    public void removeOVXPort(OVXPort ovxPort) {
        if (this.ovxPortMap.containsKey(ovxPort.getTenantId())) {
            this.ovxPortMap.remove(ovxPort.getTenantId());
        }
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (!(that instanceof PhysicalPort)) {
            return false;
        }

        PhysicalPort port = (PhysicalPort) that;
        return this.portNumber == port.portNumber
                && this.parentSwitch.getSwitchId() == port.getParentSwitch()
                        .getSwitchId();
    }

    /**
     * Gets list of virtual ports that map to this physical port for a given
     * tenant ID. If tenant ID is null then returns all virtual ports that map
     * to this port.
     *
     * @param tenant
     *            the tenant ID
     * @return list of virtual ports
     */
    public List<Map<Integer, OVXPort>> getOVXPorts(Integer tenant) {
        List<Map<Integer, OVXPort>> ports = new ArrayList<Map<Integer, OVXPort>>();
        if (tenant == null) {
            ports.addAll(this.ovxPortMap.values());
        } else {
            ports.add(this.ovxPortMap.get(tenant));
        }
        return Collections.unmodifiableList(ports);
    }

    /**
     * Changes the attribute of this port according to a MODIFY PortStatus.
     *
     * @param portstat
     *            the port status
     */
    public void applyPortStatus(OVXPortStatus portstat) {
        if (!portstat.isReason(OFPortReason.OFPPR_MODIFY)) {
            return;
        }
        OFPhysicalPort psport = portstat.getDesc();
        this.portNumber = psport.getPortNumber();
        this.hardwareAddress = psport.getHardwareAddress();
        this.name = psport.getName();
        this.config = psport.getConfig();
        this.state = psport.getState();
        this.currentFeatures = psport.getCurrentFeatures();
        this.advertisedFeatures = psport.getAdvertisedFeatures();
        this.supportedFeatures = psport.getSupportedFeatures();
        this.peerFeatures = psport.getPeerFeatures();
    }

    /**
     * Unmaps this port from the global mapping and its parent switch.
     */
    public void unregister() {
        // Remove links, if any
        if ((this.portLink != null) && (this.portLink.exists())) {
            this.portLink.egressLink.unregister();
            this.portLink.ingressLink.unregister();
        }
    }
}
