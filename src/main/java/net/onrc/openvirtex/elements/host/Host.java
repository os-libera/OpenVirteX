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
package net.onrc.openvirtex.elements.host;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort.OFPortState;

/**
 * Class representing a network host. This is a purely "Virtual" construct. A
 * host has a unique MAC address, a unique virtual port, and a unique host ID.
 * Uniqueness here is global in the OVX context. A host can also have a virtual
 * IP address which can be reused across virtual networks.
 */
public class Host implements Persistable, Component {

    /**
     * The Host's FSM. Like Link states, it is affected by a Port's status.
     */
    enum HostState {
        INIT {
            @Override
            protected void register(final Host h) {
                Host.log.debug("registering {}", h);
                try {
                    final Mappable map = OVXMap.getInstance();
                    map.addMAC(h.mac, h.port.getTenantId());
                    map.getVirtualNetwork(h.port.getTenantId()).addHost(h);
                    DBManager.getInstance().save(h);
                    h.state = HostState.INACTIVE;
                } catch (final NetworkMappingException e) {
                    Host.log.warn("tenant {} was not found in global map",
                            h.port.getTenantId());
                }
            }
        },
        INACTIVE {
            @Override
            protected boolean boot(final Host h) {
                Host.log.debug("booting {}", h);
                /*
                 * port to host goes up GIVEN port is ACTIVE. Host can attach to
                 * an inactive port.
                 */
                h.state = HostState.ACTIVE;
                h.port.setState(h.port.getState()
                        & ~OFPortState.OFPPS_LINK_DOWN.getValue());
                h.port.setEdge(true);
                return true;
            }

            @Override
            protected void unregister(final Host h) {
                Host.log.debug("un-registering {}", h);
                try {
                    final Mappable map = OVXMap.getInstance();
                    map.removeMAC(h.mac);
                    map.getVirtualNetwork(h.port.getTenantId()).removeHost(h);
                    DBManager.getInstance().remove(h);
                    h.state = HostState.STOPPED;
                } catch (final NetworkMappingException e) {
                    Host.log.warn("tenant {} was not found in global map",
                            h.port.getTenantId());
                }
            }
        },
        ACTIVE {
            @Override
            protected boolean teardown(final Host h) {
                Host.log.debug("inactivating {}", h);
                /* port to host goes down GIVEN port is ACTIVE. */
                h.state = HostState.INACTIVE;
                h.port.setState(h.port.getState()
                        | OFPortState.OFPPS_LINK_DOWN.getValue());
                return true;
            }
        },
        STOPPED;

        protected void register(final Host h) {
            Host.log.warn("Cannot register {} while status={}", h, h.state);
        }

        protected boolean boot(final Host h) {
            Host.log.warn("Cannot boot {} while status={}", h, h.state);
            return false;
        }

        protected boolean teardown(final Host h) {
            Host.log.warn("Cannot teardown {} while status={}", h, h.state);
            return false;
        }

        protected void unregister(final Host h) {
            Host.log.warn("Cannot unregister {} while status={}", h, h.state);
        }

    }

    private static Logger      log       = LogManager.getLogger(Host.class
                                                 .getName());
    /**
     * Database keyword for hosts.
     */
    public static final String DB_KEY    = "hosts";
    private final Integer      hostId;
    private final MACAddress   mac;
    /* attachment point. */
    private final OVXPort      port;
    private OVXIPAddress       ipAddress = new OVXIPAddress(0, 0);
    /* The FSM state of this host */
    private HostState          state;

    /**
     * Instantiates a new host by setting its MAC address, virtual port, and
     * host ID.
     *
     * @param mac
     *            the MAC address
     * @param port
     *            the virtual port
     * @param hostId
     *            the host ID
     */
    public Host(final MACAddress mac, final OVXPort port, final Integer hostId) {
        this.mac = mac;
        this.port = port;
        this.hostId = hostId;
        this.state = HostState.INIT;
    }

    /**
     * Sets the virtual IP address of the host.
     *
     * @param ip
     *            the virtual IP address
     */
    public void setIPAddress(final int ip) {
        this.ipAddress = new OVXIPAddress(this.port.getTenantId(), ip);
    }

    /**
     * Gets the IP address of the host.
     *
     * @return the IP address
     */
    public OVXIPAddress getIp() {
        return this.ipAddress;
    }

    /**
     * Gets the MAC address of the host.
     *
     * @return the MAC address.
     */
    public MACAddress getMac() {
        return this.mac;
    }

    /**
     * Gets the port the host is connected to.
     *
     * @return the port
     */
    public OVXPort getPort() {
        return this.port;
    }

    /**
     * Registers the host in persistent storage.
     */
    @Override
    public void register() {
        this.state.register(this);
    }

    @Override
    public Map<String, Object> getDBIndex() {
        final Map<String, Object> index = new HashMap<String, Object>();
        index.put(TenantHandler.TENANT, this.port.getTenantId());
        return index;
    }

    @Override
    public String getDBKey() {
        return Host.DB_KEY;
    }

    @Override
    public String getDBName() {
        return DBManager.DB_VNET;
    }

    @Override
    public Map<String, Object> getDBObject() {
        final Map<String, Object> dbObject = new HashMap<String, Object>();
        dbObject.put(TenantHandler.VDPID, this.port.getParentSwitch()
                .getSwitchId());
        dbObject.put(TenantHandler.VPORT, this.port.getPortNumber());
        dbObject.put(TenantHandler.MAC, this.mac.toLong());
        dbObject.put(TenantHandler.HOST, this.hostId);
        return dbObject;
    }

    /**
     * Gets the host ID.
     *
     * @return the host ID
     */
    public Integer getHostId() {
        return this.hostId;
    }

    /**
     * Unregisters the host from the persistent storage and removes all
     * references from the map.
     */
    @Override
    public void unregister() {
        this.state.unregister(this);
    }

    /**
     * Tears down the port to which the host is connected.
     */
    @Override
    public boolean tearDown() {
        return this.state.teardown(this);
    }

    @Override
    public boolean boot() {
        return this.state.boot(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.mac == null ? 0 : this.mac.hashCode());
        result = prime * result
                + (this.port == null ? 0 : this.port.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Host other = (Host) obj;
        if (this.mac == null) {
            if (other.mac != null) {
                return false;
            }
        } else
            if (!this.mac.equals(other.mac)) {
                return false;
            }
        if (this.port == null) {
            if (other.port != null) {
                return false;
            }
        } else
            if (!this.port.equals(other.port)) {
                return false;
            }
        return true;
    }

    /*
     * Converts virtual elements of a host to physical data. TODO: rewrite
     * 
     * @return map that contains host data
     */
    public HashMap<String, Object> convertToPhysical() {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hostId", this.hostId);
        map.put("dpid", this.port.getPhysicalPort().getParentSwitch()
                .getSwitchName());
        map.put("port", this.port.getPhysicalPortNumber());
        map.put("mac", this.mac.toString());

        if (this.ipAddress.getIp() != 0) {
            try {
                map.put("ipAddress",
                        OVXMap.getInstance()
                                .getPhysicalIP(this.ipAddress,
                                        this.port.getTenantId())
                                .toSimpleString());
            } catch (final AddressMappingException e) {
                Host.log.warn("Unable to fetch physical IP for host");
            }
        }
        return map;
    }

    @Override
    public String toString() {
        return "Host [HWAddr:" + this.mac.toString() + " Port:"
                + this.port.toAP() + "]";
    }

}
