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
package net.onrc.openvirtex.elements.host;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.util.MACAddress;

/**
 * A host has a unique MAC address, a unique virtual port, and
 * a unique host ID. Uniqueness here is global in the OVX context.
 * A host can also have a virtual IP address which can be reused across
 * virtual networks.
 */
public class Host implements Persistable {

    private static Logger log = LogManager.getLogger(Host.class.getName());
    /**
     * Database keyword for hosts.
     */
    public static final String DB_KEY = "hosts";
    private final Integer hostId;
    private final MACAddress mac;
    private final OVXPort port;
    private OVXIPAddress ipAddress = new OVXIPAddress(0, 0);

    /**
     * Instantiates a new host by setting its MAC address, virtual port,
     * and host ID.
     *
     * @param mac the MAC address
     * @param port the virtual port
     * @param hostId the host ID
     */
    public Host(final MACAddress mac, final OVXPort port, final Integer hostId) {
        this.mac = mac;
        this.port = port;
        this.hostId = hostId;
    }

    /**
     * Sets the virtual IP address of the host.
     *
     * @param ip the virtual IP address
     */
    public void setIPAddress(int ip) {
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
        return mac;
    }

    /**
     * Gets the port the host is connected to.
     *
     * @return the port
     */
    public OVXPort getPort() {
        return port;
    }

    /**
     * Registers the host in persistent storage.
     */
    public void register() {
        DBManager.getInstance().save(this);
    }

    @Override
    public Map<String, Object> getDBIndex() {
        Map<String, Object> index = new HashMap<String, Object>();
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
        Map<String, Object> dbObject = new HashMap<String, Object>();
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
        return hostId;
    }

    /**
     * Unregisters the host from the persistent storage and removes
     * all references from the map.
     */
    public void unregister() {
        try {
            DBManager.getInstance().remove(this);
            this.tearDown();
            Mappable map = this.port.getParentSwitch().getMap();
            map.removeMAC(this.mac);
            map.getVirtualNetwork(port.getTenantId()).removeHost(this);
        } catch (NetworkMappingException e) {
            Host.log.warn("Tried to remove host from unknown network: {}", e);
        }
    }

    /**
     * Tears down the port to which the host is connected.
     */
    public void tearDown() {
        this.port.tearDown();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mac == null) ? 0 : mac.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Host other = (Host) obj;
        if (mac == null) {
            if (other.mac != null) {
                return false;
            }
        } else if (!mac.equals(other.mac)) {
            return false;
        }
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        return true;
    }

    /*
     * Converts virtual elements of a host to physical data.
     * TODO: rewrite
     *
     * @return map that contains host data
     */
    public HashMap<String, Object> convertToPhysical() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hostId", this.hostId);
        map.put("dpid", this.port.getPhysicalPort().getParentSwitch()
                .getSwitchName());
        map.put("port", port.getPhysicalPortNumber());
        map.put("mac", this.mac.toString());

        if (this.ipAddress.getIp() != 0) {
            try {
                map.put("ipAddress", OVXMap.getInstance().getPhysicalIP(
                        this.ipAddress, this.port.getTenantId())
                                .toSimpleString());
            } catch (AddressMappingException e) {
                log.warn("Unable to fetch physical IP for host");
            }
        }
        return map;
    }

}
