/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.messages.OVXPortStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

/**
 * A physical port maintains the mapping of all virtual ports that are mapped to
 * it.
 */
public class PhysicalPort extends Port<PhysicalSwitch, PhysicalLink> implements
        Component {

    /**
     * The FSM for a port, as relevant to OVX in terms of behavior, not in terms
     * of OFPPC or OFPPS values.
     * 
     * INACTIVE implies OFPPC_PORT_DOWN (administratively down) i.e the port
     * will not receive or send traffic, and is inert but still exists. If an
     * INACTIVE port has a link connected to it, the link will also go down as
     * well; however, it will not become an edge port when it does, though it
     * will be OFPPS_LINK_DOWN.
     * 
     * A port's link may become INACTIVE or be STOPPED while the port is ACTIVE;
     * in this case, it will become an edge port.
     */
    enum PortState {
        INIT {
            @Override
            protected void register(final PhysicalPort port) {
                port.pstate = PortState.INACTIVE;
                /* port will be boot()ed from SwitchDiscoveryManager */
                if (port.parentSwitch.addPort(port)) {
                    DBManager.getInstance().addPort(port.toDPIDandPort());
                    port.log.debug("Added port {}", port.toAP());
                } else {
                    /* TODO register to try later on a wait list */
                    port.log.warn("Could not add port {}", port.toAP());
                }
            }
        },
        INACTIVE {
            @Override
            protected boolean boot(final PhysicalPort port) {
                port.log.debug("enabling port={}", port.toAP());
                port.pstate = PortState.ACTIVE;

                /* Add to discovery manager */
                PhysicalNetwork.getInstance().addPort(port);
                if (port.portLink != null && port.portLink.exists()) {
                    /* if neighbor port is ACTIVE, boot up link */
                    final PhysicalPort dst = port.portLink.egressLink
                            .getDstPort();
                    if (dst.pstate.equals(port.pstate)) {
                        port.portLink.egressLink.boot();
                        port.portLink.ingressLink.boot();
                    }
                }
                return true;
            }

            @Override
            protected void unregister(final PhysicalPort port) {
                port.log.debug("removing port={}", port.toAP());

                port.pstate = PortState.STOPPED;
                if (port.parentSwitch.removePort(port)) {
                    final PhysicalNetwork pnet = PhysicalNetwork.getInstance();
                    pnet.removePort(port);
                    DBManager.getInstance().delPort(port.toDPIDandPort());
                }

                /* remove link - calls unregister() on links */
                if (port.portLink != null && port.portLink.exists()) {
                    final PhysicalPort dst = port.portLink.egressLink
                            .getDstPort();
                    PhysicalNetwork.getInstance().removeLink(port, dst);
                    PhysicalNetwork.getInstance().removeLink(dst, port);
                }
            }
        },
        ACTIVE {
            @Override
            protected boolean teardown(final PhysicalPort port) {
                port.log.debug("disabling port={}", port.toAP());

                port.pstate = PortState.INACTIVE;
                PhysicalNetwork.getInstance().disablePort(port);

                if (port.portLink != null && port.portLink.exists()) {
                    /* it only takes one inactive port to teardown link */
                    port.portLink.egressLink.tearDown();
                    port.portLink.ingressLink.tearDown();
                }

                return true;
            }
        },
        STOPPED;

        protected void register(final PhysicalPort port) {
            port.log.debug("Port {} is already registered [state={}]",
                    port.toAP(), port.pstate);
        }

        protected boolean boot(final PhysicalPort port) {
            port.log.debug("Port {} can't be enabled from state={}",
                    port.toAP(), port.pstate);
            return false;
        }

        protected boolean teardown(final PhysicalPort port) {
            port.log.debug("Port {} can't be disabled from state={}",
                    port.toAP(), port.pstate);
            return false;
        }

        protected void unregister(final PhysicalPort port) {
            port.log.debug("Port {} can't be unregistered from state={}",
                    port.toAP(), port.pstate);
        }

    }

    protected Logger                                      log = LogManager
                                                                      .getLogger(PhysicalPort.class
                                                                              .getName());
    private final Map<Integer, HashMap<Integer, OVXPort>> ovxPortMap;
    private PortState                                     pstate;

    /**
     * Instantiates a physical port based on an OpenFlow physical port.
     *
     * @param port
     *            the OpenFlow physical port
     */
    private PhysicalPort(final OFPhysicalPort port) {
        super(port);
        this.ovxPortMap = new HashMap<Integer, HashMap<Integer, OVXPort>>();
        this.pstate = PortState.INIT;
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
        final OVXPort p = this.ovxPortMap.get(tenantId).get(vLinkId);
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
        final Map<String, Object> dbObject = new HashMap<String, Object>();
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
    public void removeOVXPort(final OVXPort ovxPort) {
        if (this.ovxPortMap.containsKey(ovxPort.getTenantId())) {
            this.ovxPortMap.remove(ovxPort.getTenantId());
        }
    }

    @Override
    public boolean equals(final Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (!(that instanceof PhysicalPort)) {
            return false;
        }
        final PhysicalPort port = (PhysicalPort) that;
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
    public List<Map<Integer, OVXPort>> getOVXPorts(final Integer tenant) {
        final List<Map<Integer, OVXPort>> ports = new ArrayList<Map<Integer, OVXPort>>();
        if (tenant == null) {
            ports.addAll(this.ovxPortMap.values());
        } else {
            ports.add(this.ovxPortMap.get(tenant));
        }
        return Collections.unmodifiableList(ports);
    }

    /**
     * Changes the attribute of this port according to a MODIFY or DELETE
     * PortStatus
     * 
     * @param portstat
     *            the PortStatus for this port
     */
    public boolean applyPortStatus(final OVXPortStatus portstat) {
        switch (OFPortReason.fromReasonCode(portstat.getReason())) {
            case OFPPR_MODIFY:
                /* link/port (en/dis)abled */
                final OFPhysicalPort psport = portstat.getDesc();
                this.portNumber = psport.getPortNumber();
                this.hardwareAddress = psport.getHardwareAddress();
                this.name = psport.getName();
                this.config = psport.getConfig();
                this.state = psport.getState();
                this.currentFeatures = psport.getCurrentFeatures();
                this.advertisedFeatures = psport.getAdvertisedFeatures();
                this.supportedFeatures = psport.getSupportedFeatures();
                this.peerFeatures = psport.getPeerFeatures();
                /* disable if link or port is down */
                if ((this.state & OFPortState.OFPPS_LINK_DOWN.getValue()) > 0
                        || (this.config & OFPortConfig.OFPPC_PORT_DOWN
                                .getValue()) > 0) {
                    this.tearDown();
                } else
                    if ((this.state & OFPortState.OFPPS_LINK_DOWN.getValue()) == 0
                            || (this.config & OFPortConfig.OFPPC_PORT_DOWN
                                    .getValue()) == 0) {
                        this.boot();
                    }
                return true;
            default:
                this.log.error("Unknown PortReason");
                return false;
        }
    }

    /**
     * unmaps this port from the global mapping and its parent switch.
     */
    @Override
    public void unregister() {
        this.pstate.unregister(this);
    }

    @Override
    public void register() {
        this.pstate.register(this);
    }

    @Override
    public boolean boot() {
        return this.pstate.boot(this);
    }

    @Override
    public boolean tearDown() {
        return this.pstate.teardown(this);
    }

    public PortState getCurState() {
        return this.pstate;
    }

}
