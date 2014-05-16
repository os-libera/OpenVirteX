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
package net.onrc.openvirtex.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.DPIDandPortPair;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.routing.RoutingAlgorithms.RoutingType;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Component that creates a previously stored virtual network when all required
 * switches, links and ports are online.
 */

public class OVXNetworkManager {

    private Map<String, Object> vnet;
    private Integer tenantId;
    // Set of offline and online physical switches
    private Set<Long> offlineSwitches;
    private Set<Long> onlineSwitches;
    // Set of offline and online physical links identified as (dpid, port number)-pair
    private Set<DPIDandPortPair> offlineLinks;
    private Set<DPIDandPortPair> onlineLinks;
    // Set of offline and online physical ports
    private Set<DPIDandPort> offlinePorts;
    private Set<DPIDandPort> onlinePorts;
    private boolean bootState;

    private static Logger log = LogManager.getLogger(OVXNetworkManager.class
            .getName());

    public OVXNetworkManager(Map<String, Object> vnet)
            throws IndexOutOfBoundException, DuplicateIndexException {
        this.vnet = vnet;
        this.tenantId = (Integer) vnet.get(TenantHandler.TENANT);
        this.offlineSwitches = new HashSet<Long>();
        this.onlineSwitches = new HashSet<Long>();
        this.offlineLinks = new HashSet<DPIDandPortPair>();
        this.onlineLinks = new HashSet<DPIDandPortPair>();
        this.offlinePorts = new HashSet<DPIDandPort>();
        this.onlinePorts = new HashSet<DPIDandPort>();
        this.bootState = false;
    }

    public Integer getTenantId() {
        return this.tenantId;
    }

    public Integer getSwitchCount() {
        return this.offlineSwitches.size() + this.onlineSwitches.size();
    }

    public Integer getLinkCount() {
        return this.offlineLinks.size() + this.onlineLinks.size();
    }

    public Integer getPortCount() {
        return this.offlinePorts.size() + this.onlinePorts.size();
    }

    public boolean getStatus() {
        return this.bootState;
    }

    /**
     * Registers switch identified by the given DPID, ensuring
     * the virtual network is spawned only after the switch is online.
     *
     * @param dpid the switch DPID
     */
    public void registerSwitch(final Long dpid) {
        this.offlineSwitches.add(dpid);
    }

    /**
     * Registers the given link, ensuring the virtual network is spawned
     * only after the link is online.
     *
     * @param dpp the link as a pair of DPID and port number
     */
    public void registerLink(final DPIDandPortPair dpp) {
        this.offlineLinks.add(dpp);
    }

    /**
     * Registers the given port, ensuring the virtual network is spawned
     * only after the port is online.
     *
     * @param port the port given as DPID and port number
     */
    public void registerPort(final DPIDandPort port) {
        this.offlinePorts.add(port);
    }

    /**
     * Changes switch from offline to online state. Creates and starts virtual
     * network if all links and switches are online.
     *
     * @param dpid the switch DPID.
     */
    public synchronized void setSwitch(final Long dpid) {
        this.offlineSwitches.remove(dpid);
        this.onlineSwitches.add(dpid);
        if (this.offlineSwitches.isEmpty() && this.offlineLinks.isEmpty()
                && this.offlinePorts.isEmpty()) {
            this.createNetwork();
        }
    }

    /**
     * Change. switch from online to offline state.
     *
     * @param dpid unique datapath id
     */
    public synchronized void unsetSwitch(final Long dpid) {
        this.offlineSwitches.add(dpid);
        this.onlineSwitches.remove(dpid);
    }

    /**
     * Change link from offline to single direction state, or from single
     * direction to online. Create and start virtual network if all links and
     * switches are online.
     *
     * @param dpp physical link given as pair of DPID and port
     */
    public synchronized void setLink(final DPIDandPortPair dpp) {
        // Link might have been set already, so check first if it's still
        // offline
        if (this.offlineLinks.contains(dpp)) {
            this.offlineLinks.remove(dpp);
            this.onlineLinks.add(dpp);
            if (this.offlineSwitches.isEmpty() && this.offlineLinks.isEmpty()
                    && this.offlinePorts.isEmpty()) {
                this.createNetwork();
            }
        }
    }

    /**
     * Changes link from online to offline state.
     *
     * @param dpp physical link given as pair of DPID and port
     */
    public synchronized void unsetLink(final DPIDandPortPair dpp) {
        if (this.onlineLinks.contains(dpp)) {
            this.onlineLinks.remove(dpp);
            this.offlineLinks.add(dpp);
        }
    }

    /**
     * Changes link from offline to single direction state, or from single
     * direction to online. Create and start virtual network if all links and
     * switches are online.
     *
     * @param key
     *            Unique link
     */
    public synchronized void setPort(final DPIDandPort port) {
        // Port might have been set already, so check first if it's still
        // offline
        if (this.offlinePorts.contains(port)) {
            this.offlinePorts.remove(port);
            this.onlinePorts.add(port);
            if (this.offlineSwitches.isEmpty() && this.offlineLinks.isEmpty()
                    && this.offlinePorts.isEmpty()) {
                this.createNetwork();
            }
        }
    }

    /**
     * Changes link from online to offline state.
     *
     * @param key
     *            Unique link
     */
    public synchronized void unsetPort(final DPIDandPort port) {
        if (this.onlinePorts.contains(port)) {
            this.onlinePorts.remove(port);
            this.offlinePorts.add(port);
        }
    }

    /**
     * Converts path in db map format to list of physical links.
     *
     * @param path the path in database map format
     * @return
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<PhysicalLink> pathToPhyLinkList(List<Map> path) {
        // Build list of physical links
        final List<PhysicalLink> result = new ArrayList<PhysicalLink>();
        for (Map<String, Object> hop : path) {
            // Get src/dst dpid & port number from map
            final Long sDpid = (Long) hop.get(TenantHandler.SRC_DPID);
            final Short sPort = ((Integer) hop.get(TenantHandler.SRC_PORT))
                    .shortValue();
            final Long dDpid = (Long) hop.get(TenantHandler.DST_DPID);
            final Short dPort = ((Integer) hop.get(TenantHandler.DST_PORT))
                    .shortValue();

            // Get physical switch instances of end points
            // TODO: what if any of the elements have gone down in the meantime?
            final PhysicalSwitch src = PhysicalNetwork.getInstance().getSwitch(
                    sDpid);
            final PhysicalSwitch dst = PhysicalNetwork.getInstance().getSwitch(
                    dDpid);

            // Get physical link instance
            final PhysicalLink phyLink = PhysicalNetwork.getInstance().getLink(
                    src.getPort(sPort), dst.getPort(dPort));

            result.add(phyLink);
        }
        return result;
    }

    /**
     * Creates OVX network and elements based on persistent storage, boots
     * network afterwards.
     * TODO: proper error handling (roll-back?).
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createNetwork() {

        OVXNetworkManager.log.info("Virtual network {} ready for boot",
                this.tenantId);

        // Create OVX network
        final ArrayList<String> ctrlUrls = (ArrayList<String>) this.vnet
                .get(TenantHandler.CTRLURLS);
        final Integer network = (Integer) this.vnet.get(TenantHandler.NETADD);
        final IPAddress addr = new OVXIPAddress(network, -1);
        final Short netMask = ((Integer) this.vnet.get(TenantHandler.NETMASK))
                .shortValue();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = new OVXNetwork(this.tenantId, ctrlUrls, addr,
                    netMask);
        } catch (IndexOutOfBoundException e) {
            OVXNetworkManager.log.error(
                    "Error recreating virtual network {} from database",
                    this.tenantId);
            return;
        }
        virtualNetwork.register();

        // Create OVX switches
        final List<Map<String, Object>> switches = (List<Map<String, Object>>) this.vnet
                .get(Switch.DB_KEY);
        if (switches != null) {
            for (Map<String, Object> sw : switches) {
                List<Long> dpids = (List<Long>) sw.get(TenantHandler.DPIDS);
                long switchId = (long) sw.get(TenantHandler.VDPID);
                try {
                    virtualNetwork.createSwitch(dpids, switchId);
                } catch (IndexOutOfBoundException e) {
                    OVXNetworkManager.log.error(
                            "Error recreating virtual switch {} from database",
                            switchId);
                    continue;
                }
            }
        }

        // Create OVX ports
        final List<Map<String, Object>> ports = (List<Map<String, Object>>) this.vnet
                .get(Port.DB_KEY);
        if (ports != null) {
            for (Map<String, Object> port : ports) {
                long physicalDpid = (Long) port.get(TenantHandler.DPID);
                short portNumber = ((Integer) port.get(TenantHandler.PORT))
                        .shortValue();
                short vportNumber = ((Integer) port.get(TenantHandler.VPORT))
                        .shortValue();
                try {
                    virtualNetwork.createPort(physicalDpid, portNumber,
                            vportNumber);
                } catch (IndexOutOfBoundException e) {
                    OVXNetworkManager.log.error(
                            "Error recreating virtual port {} from database",
                            vportNumber);
                    continue;
                }
            }
        }

        // Create OVX big switch routes if manual
        if (switches != null) {
            for (Map<String, Object> sw : switches) {
                long switchId = (long) sw.get(TenantHandler.VDPID);
                String alg = (String) sw.get(TenantHandler.ALGORITHM);
                Integer backups = (Integer) sw.get(TenantHandler.BACKUPS);
                // Don't bother with switch routes if not a bigswitch
                if ((alg == null) || (backups == null)) {
                    continue;
                }
                try {
                    virtualNetwork.setOVXBigSwitchRouting(switchId, alg,
                            backups.byteValue());
                } catch (RoutingAlgorithmException e) {
                    OVXNetworkManager.log.error(
                            "Error setting routing mode for switch {} from database", switchId);
                    continue;
                }

                // Only restore routes if manual routing
                // Remove all stored routes otherwise
                if (alg == RoutingType.NONE.name()) {
                    final List<Map<String, Object>> routes = (List<Map<String, Object>>) this.vnet
                            .get(SwitchRoute.DB_KEY);
                    // List of created routeId's per switch
                    final Map<Long, List<Integer>> routeIds = new HashMap<Long, List<Integer>>();
                    if (routes != null) {
                        for (Map<String, Object> route : routes) {
                            long dpid = (Long) route.get(TenantHandler.VDPID);
                            short srcPort = ((Integer) route
                                    .get(TenantHandler.SRC_PORT)).shortValue();
                            short dstPort = ((Integer) route
                                    .get(TenantHandler.DST_PORT)).shortValue();
                            byte priority = ((Integer) route
                                    .get(TenantHandler.PRIORITY)).byteValue();
                            int routeId = (Integer) route
                                    .get(TenantHandler.ROUTE);
                            // Maintain id's of routes per switch so we don't
                            // create reverse
                            List<Integer> visited = routeIds.get(dpid);
                            if (visited == null) {
                                visited = new ArrayList<Integer>();
                                routeIds.put(dpid, visited);
                            }
                            if (visited.contains(routeId)) {
                                continue;
                            } else {
                                visited.add(routeId);
                            }

                            List<Map> path = (List<Map>) route
                                    .get(TenantHandler.PATH);
                            List<PhysicalLink> physicalLinks = this
                                    .pathToPhyLinkList(path);

                            try {
                                virtualNetwork.connectRoute(dpid, srcPort,
                                        dstPort, physicalLinks, priority,
                                        routeId);
                            } catch (IndexOutOfBoundException e) {
                                OVXNetworkManager.log
                                        .error("Error recreating virtual switch route {} from database",
                                                routeId);
                                continue;
                            }
                        }
                    }
                } else {
                    DBManager.getInstance()
                            .removeSwitchPath(tenantId, switchId);
                }
            }
        }

        // Create OVX links
        final List<Map<String, Object>> links = (List<Map<String, Object>>) this.vnet
                .get(Link.DB_KEY);
        // Maintain link id's of virtual links we have created - ensure reverse
        // link is not created again
        final List<Integer> linkIds = new ArrayList<Integer>();
        if (links != null) {
            for (Map<String, Object> link : links) {
                // Skip link if we already handled the reverse
                Integer linkId = (Integer) link.get(TenantHandler.LINK);
                if (linkIds.contains(linkId)) {
                    continue;
                } else {
                    linkIds.add(linkId);
                }
                // Obtain virtual src and dst dpid/port, priority
                Long srcDpid = (Long) link.get(TenantHandler.SRC_DPID);
                Short srcPort = ((Integer) link.get(TenantHandler.SRC_PORT))
                        .shortValue();
                Long dstDpid = (Long) link.get(TenantHandler.DST_DPID);
                Short dstPort = ((Integer) link.get(TenantHandler.DST_PORT))
                        .shortValue();
                Byte priority = ((Integer) link.get(TenantHandler.PRIORITY))
                        .byteValue();
                String alg = (String) link.get(TenantHandler.ALGORITHM);
                byte backups = ((Integer) link.get(TenantHandler.BACKUPS))
                        .byteValue();

                // Build list of physical links
                List<Map> path = (List<Map>) link.get(TenantHandler.PATH);
                List<PhysicalLink> physicalLinks = this.pathToPhyLinkList(path);

                // Create virtual link
                try {
                    DBManager.getInstance().removeLinkPath(tenantId, linkId);

                    virtualNetwork.connectLink(srcDpid, srcPort, dstDpid,
                            dstPort, alg, backups, linkId);

                    // Only configure path if manual routing mode
                    if (alg == RoutingType.NONE.getValue()) {
                        virtualNetwork.setLinkPath(linkId, physicalLinks,
                                priority);
                    }

                } catch (IndexOutOfBoundException | PortMappingException e) {
                    OVXNetworkManager.log.error(
                            "Error recreating virtual link {} from database",
                            linkId);
                    continue;
                }
            }
        }

        // Connect hosts
        final List<Map<String, Object>> hosts = (List<Map<String, Object>>) this.vnet
                .get(Host.DB_KEY);
        if (hosts != null) {
            for (Map<String, Object> host : hosts) {
                final long dpid = (Long) host.get(TenantHandler.VDPID);
                final short port = ((Integer) host.get(TenantHandler.VPORT))
                        .shortValue();
                final MACAddress macAddr = MACAddress.valueOf((Long) host
                        .get(TenantHandler.MAC));
                final int hostId = (Integer) host.get(TenantHandler.HOST);
                try {
                    virtualNetwork.connectHost(dpid, port, macAddr, hostId);
                } catch (IndexOutOfBoundException e) {
                    OVXNetworkManager.log.error("Failed to create host {}",
                            hostId);
                    continue;
                }
            }
        }

        // Start network
        virtualNetwork.boot();
        this.bootState = true;
    }
}
