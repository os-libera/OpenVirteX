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
package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

/**
 * This singleton class maintains all the virtual-to-physical and reverse mappings.
 * These encompass switch mappings, link mappings, switch route mappings,
 * the IP address mappings, the tenant ID to virtual network mapping, and the
 * list of MAC addresses.
 */
public final class OVXMap implements Mappable {

    private static Logger log = LogManager.getLogger(OVXMap.class.getName());
    private static AtomicReference<OVXMap> mapInstance = new AtomicReference<>();

    private ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>> virtualSwitchMap;
    private ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>> physicalSwitchMap;
    private ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>> virtualLinkMap;
    private ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, List<OVXLink>>> physicalLinkMap;
    private ConcurrentHashMap<SwitchRoute, ArrayList<PhysicalLink>> routetoPhyLinkMap;
    private ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, Set<SwitchRoute>>> phyLinktoRouteMap;
    private ConcurrentHashMap<Integer, OVXNetwork> networkMap;
    private RadixTree<OVXIPAddress> physicalIPMap;
    private RadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>> virtualIPMap;
    private RadixTree<Integer> macMap;

    /**
     * Creates a new map instance, by initializing all mapping data structures.
     */
    private OVXMap() {
        this.virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>();
        this.physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>>();
        this.virtualLinkMap = new ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>();
        this.physicalLinkMap = new ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, List<OVXLink>>>();
        this.routetoPhyLinkMap = new ConcurrentHashMap<SwitchRoute, ArrayList<PhysicalLink>>();
        this.phyLinktoRouteMap = new ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, Set<SwitchRoute>>>();
        this.networkMap = new ConcurrentHashMap<Integer, OVXNetwork>();
        this.physicalIPMap = new ConcurrentRadixTree<OVXIPAddress>(
                new DefaultCharArrayNodeFactory());
        this.virtualIPMap = new ConcurrentRadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>>(
                new DefaultCharArrayNodeFactory());
        this.macMap = new ConcurrentRadixTree<Integer>(
                new DefaultCharArrayNodeFactory());
    }

    /**
     * Gets the instance of the class and if this already exists
     * then the existing object will be returned. This is a singleton class.
     *
     * @return mapInstance Return the OVXMap object instance
     */
    public static OVXMap getInstance() {
        OVXMap.mapInstance.compareAndSet(null, new OVXMap());
        return OVXMap.mapInstance.get();
    }

    /**
     * Resets the map instance. Do not use!
     */
    public static void reset() {
        OVXMap.log
                .debug("OVXMap has been reset explicitly. Hope you know what you are doing!");
        OVXMap.mapInstance.set(null);
    }

    // ADD objects to dictionary

    /**
     * Creates the mapping between PhysicalSwithes and a VirtualSwitch. This
     * function takes in a list of physicalSwitches and adds to the OVXMap
     * indexed by the virtualSwitch.
     *
     * @param physicalSwitches the list of physical switches
     * @param virtualSwitch the virtual switch
     */
    @Override
    public void addSwitches(final List<PhysicalSwitch> physicalSwitches,
            final OVXSwitch virtualSwitch) {
        for (final PhysicalSwitch physicalSwitch : physicalSwitches) {
            this.addSwitch(physicalSwitch, virtualSwitch);
        }
    }

    /**
     * Creates the mapping between OVX switch to physical switch and from
     * physical switch to virtual switch.
     *
     * @param physicalSwitch
     *            Refers to the PhysicalSwitch from the PhysicalNetwork
     * @param virtualSwitch
     *            Has type OVXSwitch and this switch is specific to a tenantId
     *
     */
    private void addSwitch(final PhysicalSwitch physicalSwitch,
            final OVXSwitch virtualSwitch) {
        this.addPhysicalSwitch(physicalSwitch, virtualSwitch);
        this.addVirtualSwitch(virtualSwitch, physicalSwitch);
    }

    /**
     * Creates the mapping between PhysicalLinks and a VirtualLink. This function
     * takes in a list of physicalLinks rather than an individual physicalLink
     * and adds the list to the OVXmap.
     *
     * @param physicalLinks the list of physical links
     * @param virtualLink the virtual link
     */
    @Override
    public void addLinks(final List<PhysicalLink> physicalLinks,
            final OVXLink virtualLink) {
        for (final PhysicalLink physicalLink : physicalLinks) {
            this.addLink(physicalLink, virtualLink);
        }
    }

    /**
     * Creates the mapping between the virtual link to physical link and physical
     * link to virtual link.
     *
     * @param physicalLink
     *            Refers to the PhysicalLink in the PhysicalNetwork
     * @param virtualLink
     *            Refers to the OVXLink which consists of many PhysicalLinks and
     *            specific to a tenantId
     *
     */
    private void addLink(final PhysicalLink physicalLink,
            final OVXLink virtualLink) {
        this.addPhysicalLink(physicalLink, virtualLink);
        this.addVirtualLink(virtualLink, physicalLink);
    }

    public Iterable<CharSequence> getAllKeys() {
        return this.virtualIPMap.getClosestKeys("");
    }

    /**
     * This is the generic function which takes as arguments the
     * PhysicalIPAddress and the OVXIPAddress. This will add the value into both
     * the physical to virtual map and in the other direction.
     *
     * @param physicalIP
     *            Refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     * @param virtualIP
     *            The IP address used within the VirtualNetwork
     */
    @Override
    public void addIP(final PhysicalIPAddress physicalIP,
            final OVXIPAddress virtualIP) {
        this.addPhysicalIP(physicalIP, virtualIP);
        this.addVirtualIP(virtualIP, physicalIP);
    }

    /**
     * This function will create a map indexed on the key PhysicalIPAddress with
     * value OVXIPAddress.
     *
     * @param physicalIP
     *            refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     * @param virtualIP
     *            the IP address used within the VirtualNetwork
     */
    private void addPhysicalIP(final PhysicalIPAddress physicalIP,
            final OVXIPAddress virtualIP) {
        this.physicalIPMap.put(physicalIP.toString(), virtualIP);
    }

    /**
     * Creates a map indexed on the key OVXIPAddress with
     * value a ConcurrentHashMap mapping the tenant id to the PhysicalIPAddress.
     *
     * @param virtualIP
     *            the IP address used within the VirtualNetwork
     * @param physicalIP
     *            refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     */
    private void addVirtualIP(final OVXIPAddress virtualIP,
            final PhysicalIPAddress physicalIP) {
        ConcurrentHashMap<Integer, PhysicalIPAddress> ipMap = this.virtualIPMap
                .getValueForExactKey(virtualIP.toString());
        if (ipMap == null) {
            ipMap = new ConcurrentHashMap<Integer, PhysicalIPAddress>();
            this.virtualIPMap.put(virtualIP.toString(), ipMap);
        }
        ipMap.put(virtualIP.getTenantId(), physicalIP);
    }

    /**
     * Sets up the mapping from the physicalSwitch to the tenant
     * ID and virtualSwitch which has been specified.
     *
     * @param physicalSwitch
     *            A PhysicalSwitch object which is found in the PhysicalNetwork
     * @param virtualSwitch
     *            An OVXSwitch object which is found in the OVXNetwork
     *
     */
    private void addPhysicalSwitch(final PhysicalSwitch physicalSwitch,
            final OVXSwitch virtualSwitch) {
        ConcurrentHashMap<Integer, OVXSwitch> switchMap = this.physicalSwitchMap
                .get(physicalSwitch);
        if (switchMap == null) {
            switchMap = new ConcurrentHashMap<Integer, OVXSwitch>();
            this.physicalSwitchMap.put(physicalSwitch, switchMap);
        }
        switchMap.put(virtualSwitch.getTenantId(), virtualSwitch);
    }

    /**
     * Sets up the mapping from the physical link to the OVXLinks which contain
     * the given physical link.
     *
     * @param virtualLink
     *            OVXLink contains the OVXPort and OVXSwitch for source and
     *            destination in the OVXNetwork
     */
    private void addPhysicalLink(final PhysicalLink physicalLink,
            final OVXLink virtualLink) {
        ConcurrentHashMap<Integer, List<OVXLink>> linkMap = this.physicalLinkMap
                .get(physicalLink);
        if (linkMap == null) {
            linkMap = new ConcurrentHashMap<Integer, List<OVXLink>>();
            this.physicalLinkMap.put(physicalLink, linkMap);
        }
        List<OVXLink> linkList = linkMap.get(virtualLink.getTenantId());
        if (linkList == null) {
            linkList = new ArrayList<OVXLink>();
            linkMap.put(virtualLink.getTenantId(), linkList);
        }
        linkList.add(virtualLink);
    }

    /**
     * Sets up the mapping from the OVXSwitch to the physicalSwitch which has
     * been specified.
     *
     * @param virtualSwitch
     *            A OVXSwitch object which represents a single switch in the
     *            OVXNetwork
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     *
     */
    private void addVirtualSwitch(final OVXSwitch virtualSwitch,
            final PhysicalSwitch physicalSwitch) {
        ArrayList<PhysicalSwitch> switchList = this.virtualSwitchMap
                .get(virtualSwitch);
        if (switchList == null) {
            switchList = new ArrayList<PhysicalSwitch>();
            this.virtualSwitchMap.put(virtualSwitch, switchList);
        }
        switchList.add(physicalSwitch);
    }

    /**
     * Maps the virtual link to the physical links that it contains.
     *
     * @param virtualLink
     *            A OVXLink object which represents a single link in the
     *            OVXNetwork
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     */
    private void addVirtualLink(final OVXLink virtualLink,
            final PhysicalLink physicalLink) {
        ArrayList<PhysicalLink> linkList = this.virtualLinkMap.get(virtualLink);
        if (linkList == null) {
            linkList = new ArrayList<PhysicalLink>();
            this.virtualLinkMap.put(virtualLink, linkList);
        }
        linkList.add(physicalLink);
    }

    /**
     * Maintain a list of all the virtualNetworks in the system indexed by the
     * tenant id mapping to OVXNetworks.
     *
     * @param virtualNetwork
     *            An OVXNetwork object which keeps track of all the elements in
     *            the Virtual network
     *
     */
    @Override
    public void addNetwork(final OVXNetwork virtualNetwork) {
        this.networkMap.put(virtualNetwork.getTenantId(), virtualNetwork);
    }

    @Override
    public void addMAC(final MACAddress mac, final Integer tenantId) {
        this.macMap.put(mac.toStringNoColon(), tenantId);
    }

    @Override
    public void addRoute(final SwitchRoute route,
            final List<PhysicalLink> physicalLinks) {
        route.setPathSrcPort(physicalLinks.get(0).getSrcPort());
        route.setPathDstPort(physicalLinks.get(physicalLinks.size() - 1)
                .getDstPort());
        this.addRoutetoLink(route, physicalLinks);
        for (PhysicalLink l : physicalLinks) {
            this.addLinktoRoute(l, route);
        }
    }

    /**
     * Adds given route to list of physical links.
     *
     * @param route the route
     * @param links list of physical links
     */
    private void addRoutetoLink(SwitchRoute route, List<PhysicalLink> links) {
        ArrayList<PhysicalLink> path = this.routetoPhyLinkMap.get(route);
        if (path == null) {
            path = new ArrayList<PhysicalLink>();
            this.routetoPhyLinkMap.put(route, path);
        }
        for (PhysicalLink l : links) {
            path.add(l);
        }
    }

    /**
     * Adds given physical link to the route.
     *
     * @param link the physical link
     * @param route the route
     */
    private void addLinktoRoute(PhysicalLink link, SwitchRoute route) {
        ConcurrentHashMap<Integer, Set<SwitchRoute>> rmap = this.phyLinktoRouteMap
                .get(link);
        if (rmap == null) {
            rmap = new ConcurrentHashMap<Integer, Set<SwitchRoute>>();
            this.phyLinktoRouteMap.put(link, rmap);
        }
        Set<SwitchRoute> rlist = rmap.get(route.getTenantId());
        if (rlist == null) {
            rlist = new HashSet<SwitchRoute>();
            rmap.put(route.getTenantId(), rlist);
        }
        rlist.add(route);
    }

    // Access objects from dictionary given the key

    @Override
    public PhysicalIPAddress getPhysicalIP(final OVXIPAddress ip,
            final Integer tenantId) throws AddressMappingException {
        final ConcurrentHashMap<Integer, PhysicalIPAddress> ips = this.virtualIPMap
                .getValueForExactKey(ip.toString());
        if (ips == null) {
            throw new AddressMappingException(ip, PhysicalIPAddress.class);
        }
        PhysicalIPAddress pip = ips.get(tenantId);
        if (pip == null) {
            throw new AddressMappingException(tenantId, PhysicalIPAddress.class);
        }
        return pip;
    }

    @Override
    public OVXIPAddress getVirtualIP(final PhysicalIPAddress ip)
            throws AddressMappingException {
        OVXIPAddress vip = this.physicalIPMap
                .getValueForExactKey(ip.toString());
        if (vip == null) {
            throw new AddressMappingException(ip, OVXIPAddress.class);
        }
        return vip;
    }

    /**
     * Gets the OVXSwitch which has been specified by the physicalSwitch and
     * tenantId.
     *
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     * @param tenantId the tenant ID
     * @return virtualSwitch the virtual switch
     * @throws SwitchMappingException if the physical switch is invalid
     */
    @Override
    public OVXSwitch getVirtualSwitch(final PhysicalSwitch physicalSwitch,
            final Integer tenantId) throws SwitchMappingException {
        final ConcurrentHashMap<Integer, OVXSwitch> sws = this.physicalSwitchMap
                .get(physicalSwitch);
        if (sws == null) {
            throw new SwitchMappingException(physicalSwitch, OVXSwitch.class);
        }
        OVXSwitch vsw = sws.get(tenantId);
        if (vsw == null) {
            throw new SwitchMappingException(tenantId, OVXSwitch.class);
        }
        return vsw;
    }

    /**
     * Gets the list of OVXLinks that are part of virtual network identified by
     * tenantId and which include the specified physicalLink.
     *
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     * @param tenantId the tenant ID
     * @return virtualLink A OVXLink object which represents a single link in
     *         the OVXNetwork
     * @throws LinkMappingException if the physical link is invalid
     */
    @Override
    public List<OVXLink> getVirtualLinks(final PhysicalLink physicalLink,
            final Integer tenantId) throws LinkMappingException {
        final ConcurrentHashMap<Integer, List<OVXLink>> linkMap = this.physicalLinkMap
                .get(physicalLink);
        if (linkMap == null) {
            throw new LinkMappingException(physicalLink, OVXLink.class);
        }
        final List<OVXLink> linkList = linkMap.get(tenantId);
        if (linkList == null) {
            throw new LinkMappingException(tenantId, OVXLink.class);
        }
        return linkList;
    }

    /**
     * Gets the physicalLinks that all make up a specified OVXLink. Return a list
     * of all the physicalLinks that make up the OVXLink.
     *
     * @param virtualLink
     *            An OVXLink object which represents a single link in the
     *            OVXNetwork
     * @return physicalLinks A List of PhysicalLink objects which represent a
     *         single source and destination PhysicalPort and PhysicalSwitch
     * @throws LinkMappingException if the virtual link is invalid
     */
    @Override
    public List<PhysicalLink> getPhysicalLinks(final OVXLink virtualLink)
            throws LinkMappingException {
        List<PhysicalLink> linkList = this.virtualLinkMap.get(virtualLink);
        if (linkList == null) {
            throw new LinkMappingException(virtualLink, PhysicalLink.class);
        }
        return linkList;
    }

    /**
     * Gets the physical switches that are contained in the OVXSwitch. For a big
     * switch this will be multiple physical switches.
     *
     * @param virtualSwitch
     *            A OVXSwitch object representing a single switch in the virtual
     *            network
     * @return physicalSwitches A List of PhysicalSwitch objects that are each
     *         part of the OVXSwitch specified
     * @throws SwitchMappingException if the virtual switch is invalid
     */
    @Override
    public List<PhysicalSwitch> getPhysicalSwitches(
            final OVXSwitch virtualSwitch) throws SwitchMappingException {
        List<PhysicalSwitch> pswList = this.virtualSwitchMap.get(virtualSwitch);
        if (pswList == null) {
            throw new SwitchMappingException(virtualSwitch,
                    PhysicalSwitch.class);
        }
        return this.virtualSwitchMap.get(virtualSwitch);
    }

    /**
     * Gets the virtual network instance based on the tenant ID.
     *
     * @param tenantId
     *            This is an Integer that represents a unique number for each
     *            virtualNetwork
     * @return virtualNetwork A OVXNetwork object that represents all the
     *         information related to a virtual network
     * @throws NetworkMappingException if the tenant ID is invalid
     */
    @Override
    public OVXNetwork getVirtualNetwork(final Integer tenantId)
            throws NetworkMappingException {
        OVXNetwork vnet = this.networkMap.get(tenantId);
        if (vnet == null) {
            throw new NetworkMappingException(tenantId);
        }
        return vnet;
    }

    @Override
    public Integer getMAC(final MACAddress mac) throws AddressMappingException {
        Integer macint = this.macMap.getValueForExactKey(mac.toStringNoColon());
        if (macint == null) {
            throw new AddressMappingException("Given Key " + mac
                    + " not mapped to any values");
        }
        return macint;
    }

    @Override
    public Map<Integer, OVXNetwork> listVirtualNetworks() {
        return Collections.unmodifiableMap(this.networkMap);
    }

    // Remove objects from dictionary

    /**
     * Removes the virtual network from the map.
     *
     * @param network the virtual network instance
     */
    public void removeNetwork(OVXNetwork network) {
        int tenantId = network.getTenantId();
        if (this.networkMap.get(tenantId) != null) {
            this.networkMap.remove(tenantId);
        }
    }

    /**
     * Removes the virtual link from the map.
     *
     * @param virtualLink the virtual link
     */
    public void removeVirtualLink(OVXLink virtualLink) {
        if (this.virtualLinkMap.containsKey(virtualLink)) {
            List<PhysicalLink> physicalLinks = Collections
                    .unmodifiableList(this.virtualLinkMap.get(virtualLink));
            for (PhysicalLink physicalLink : physicalLinks) {
                if (this.physicalLinkMap.get(physicalLink).containsKey(
                        virtualLink.getTenantId())) {
                    this.physicalLinkMap.get(physicalLink).remove(
                            virtualLink.getTenantId());
                }
            }
            this.virtualLinkMap.remove(virtualLink);
        }
    }

    @Override
    public void removePhysicalLink(PhysicalLink physicalLink) {
        Map<Integer, List<OVXLink>> lmap = this.physicalLinkMap
                .get(physicalLink);
        Map<Integer, Set<SwitchRoute>> rmap = this.phyLinktoRouteMap
                .get(physicalLink);
        this.removePhysicalLink(lmap, this.virtualLinkMap, physicalLink);
        this.removePhysicalLink(rmap, this.routetoPhyLinkMap, physicalLink);
        this.physicalLinkMap.remove(physicalLink);
        this.phyLinktoRouteMap.remove(physicalLink);
    }

    /**
     * Helper function for removing PhysicalLinks from mappings.
     *
     * @param submap
     *            Map containing a Collection of Links mapped to PhysicalLink
     *            phylink
     * @param map Mapping to remove phylink from
     * @param phylink the PhysicalLink to remove
     */
    private <T> void removePhysicalLink(
            Map<Integer, ? extends Collection<T>> submap,
            ConcurrentHashMap<T, ? extends Collection<PhysicalLink>> map,
            PhysicalLink phylink) {
        if (submap == null) {
            return;
        }
        for (Map.Entry<Integer, ? extends Collection<T>> el : submap.entrySet()) {
            for (T link : el.getValue()) {
                map.get(link).remove(phylink);
            }
        }
    }

    @Override
    public void removeVirtualSwitch(OVXSwitch virtualSwitch) {
        if (this.virtualSwitchMap.containsKey(virtualSwitch)) {
            ArrayList<PhysicalSwitch> physicalSwitches = this.virtualSwitchMap
                    .get(virtualSwitch);
            for (PhysicalSwitch physicalSwitch : physicalSwitches) {
                if (this.physicalSwitchMap.get(physicalSwitch).containsKey(
                        virtualSwitch.getTenantId())) {
                    this.physicalSwitchMap.get(physicalSwitch).remove(
                            virtualSwitch.getTenantId());
                }
            }
            this.virtualSwitchMap.remove(virtualSwitch);
        }
    }

    @Override
    public void removeVirtualIPs(int tenantId) {
        ArrayList<String> physicalIPs = new ArrayList<String>();
        for (ConcurrentHashMap<Integer, PhysicalIPAddress> map : virtualIPMap
                .getValuesForKeysStartingWith("")) {
            if (map.containsKey(tenantId)) {
                physicalIPs.add(map.get(tenantId).toString());
                map.remove(tenantId);
            }
        }

        for (String physicalIP : physicalIPs) {
            physicalIPMap.remove(physicalIP);
        }
    }

    /**
     * Removes MAC address from the map.
     *
     * @param mac the MAC address
     */
    public void removeMAC(final MACAddress mac) {
        this.macMap.remove(mac.toStringNoColon());
    }

    @Override
    public List<PhysicalLink> getRoute(SwitchRoute route)
            throws LinkMappingException {
        List<PhysicalLink> plList = this.routetoPhyLinkMap.get(route);
        if (plList == null) {
            throw new LinkMappingException(route, PhysicalLink.class);
        }
        return plList;
    }

    @Override
    public Set<SwitchRoute> getSwitchRoutes(PhysicalLink physicalLink,
            Integer tenantId) throws LinkMappingException {
        Map<Integer, Set<SwitchRoute>> pair = this.phyLinktoRouteMap
                .get(physicalLink);
        if (pair == null) {
            throw new LinkMappingException(physicalLink, SwitchRoute.class);
        }
        Set<SwitchRoute> rList = pair.get(tenantId);
        if (rList == null) {
            throw new LinkMappingException(tenantId, SwitchRoute.class);
        }
        return rList;
    }

    @Override
    public void removeRoute(SwitchRoute route) {
        List<PhysicalLink> plist = this.routetoPhyLinkMap.get(route);
        if (plist == null) {
            return;
        }
        int tid = route.getTenantId();
        for (PhysicalLink l : plist) {
            removeRoute(l, tid, route);
        }
        this.routetoPhyLinkMap.remove(route);
    }

    /**
     * Removes the given route that use the given physical link from the map.
     *
     * @param link the physical link
     * @param tid the tenant ID
     * @param route the switch route
     */
    private void removeRoute(PhysicalLink link, int tid, SwitchRoute route) {
        Map<Integer, Set<SwitchRoute>> rmap = this.phyLinktoRouteMap.get(link);
        if (rmap == null) {
            return;
        }
        Set<SwitchRoute> routes = rmap.get(tid);
        if (routes != null) {
            routes.remove(route);
            /* clean up any empty maps */
            if (routes.isEmpty()) {
                rmap.remove(tid);
            }
            if (rmap.isEmpty()) {
                this.phyLinktoRouteMap.remove(link);
            }
        }
    }

    @Override
    public void removePhysicalSwitch(PhysicalSwitch physicalSwitch) {
        Map<Integer, OVXSwitch> switches = this.physicalSwitchMap
                .get(physicalSwitch);
        if (switches == null) {
            return;
        }
        List<PhysicalSwitch> psw;
        Iterator<Map.Entry<Integer, OVXSwitch>> iter = switches.entrySet()
                .iterator();
        while (iter.hasNext()) {
            psw = this.virtualSwitchMap.get(iter.next().getValue());
            if (psw == null) {
                continue;
            }
            psw.remove(physicalSwitch);
            if (psw.isEmpty()) {
                /*
                 * no physicalSwitches under vswitch - signal to it eventually
                 * for now remove the vswitch
                 */
                iter.remove();
            }
        }
        this.physicalSwitchMap.remove(physicalSwitch);
    }

    // Below: helper functions needed to avoid using error exception for flow control

    /**
     * Checks if the given virtual IP address is mapped to a physical IP
     * in the virtual network identified by the tenant ID.
     *
     * @param vip the virtual IP address
     * @param tenantId the tenant ID
     * @return true if the mapping exists, false otherwise
     */
    public boolean hasPhysicalIP(OVXIPAddress vip, Integer tenantId) {
        final ConcurrentHashMap<Integer, PhysicalIPAddress> ips = this.virtualIPMap
                .getValueForExactKey(vip.toString());
        return (ips != null) && (ips.get(tenantId) != null);
    }

    @Override
    public boolean hasVirtualIP(PhysicalIPAddress ip) {
        return this.physicalIPMap.getValueForExactKey(ip.toString()) != null;
    }

    /**
     * Checks if the given MAC address is registered in the map.
     *
     * @param mac the MAC address
     * @return true if the MAC is registered, false otherwise
     */
    public boolean hasMAC(MACAddress mac) {
        return this.macMap.getValueForExactKey(mac.toStringNoColon()) != null;
    }

    /**
     * Checks if the given physical link is part of a virtual switch route
     * in the virtual network identified by the tenant ID.
     *
     *  @param physicalLink the physical link
     *  @param tenantId the tenant Id
     *  @return true if the physical link is part of a virtual switch route,
     *  false otherwise
     */
    public boolean hasSwitchRoutes(final PhysicalLink physicalLink,
            final Integer tenantId) {
        Map<Integer, Set<SwitchRoute>> pair = this.phyLinktoRouteMap
                .get(physicalLink);
        return (pair != null) && (pair.get(tenantId) != null);
    }

    /**
     * Checks if the given physical link is part of a virtual link in the
     * virtual network identified by the tenant ID.
     *
     * @param physicalLink the physical link
     * @param tenantId the tenant ID
     * @return true if the physical link is part of a virtual link, false otherwise
     */
    public boolean hasOVXLinks(final PhysicalLink physicalLink,
            final Integer tenantId) {
        final Map<Integer, List<OVXLink>> pair = this.physicalLinkMap
                .get(physicalLink);
        return (pair != null) && (pair.get(tenantId) != null);
    }

    @Override
    public boolean hasVirtualSwitch(PhysicalSwitch physicalSwitch, int tenantId) {
        final ConcurrentHashMap<Integer, OVXSwitch> sws = this.physicalSwitchMap
                .get(physicalSwitch);
        return (sws != null) && (sws.get(tenantId) != null);
    }

    @Override
    public void knownLink(PhysicalLink that) {
        Enumeration<PhysicalLink> links = physicalLinkMap.keys();
        while (links.hasMoreElements()) {
            PhysicalLink link = links.nextElement();
            if (link.equals(that)) {
                that.setLinkId(link.getLinkId());
                return;
            }
        }
    }

}
