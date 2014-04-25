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

import java.util.List;
import java.util.Map;
import java.util.Set;

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

public interface Mappable {
    // ADD objects to dictionary

    /**
     * This function creates the map between the PhysicalIP and VirtualIP in
     * both directions.
     *
     * @param physicalIP
     *            Refers to the PhysicalIPAddress which is created using the
     *            tenant id and virtualIP
     * @param virtualIP
     *            The IP address used within the VirtualNetwork
     */
    public void addIP(PhysicalIPAddress physicalIP, OVXIPAddress virtualIP);

    /**
     * Create the mapping between PhysicalSwithes and a VirtualSwitch. This
     * function takes in a list of physicalSwitches and adds to the OVXMap
     * indexed by the virtualSwitch.
     *
     * @param physicalSwitches
     * @param virtualSwitch
     */
    public void addSwitches(final List<PhysicalSwitch> physicalSwitches,
            final OVXSwitch virtualSwitch);

    /**
     * Create the mapping between PhysicalLinks and a VirtualLink. This function
     * takes in a list of physicalLinks rather than an individual physicalLink
     * and adds the list to the OVXmap.
     *
     * @param physicalLinks
     * @param virtualLink
     */
    public void addLinks(final List<PhysicalLink> physicalLinks,
            final OVXLink virtualLink);

    /**
     * Maintains a list of all the virtualNetworks in the system indexed by the
     * tenant id mapping to VirtualNetworks.
     *
     * @param virtualNetwork
     *            An OVXNetwork object which keeps track of all the elements in
     *            the Virtual network
     */
    public void addNetwork(OVXNetwork virtualNetwork);

    /**
     * Creates the maping between the MAC address and tenant ID.
     *
     * @param mac the MAC address
     * @param tenantId the tenant ID.
     */
    public void addMAC(MACAddress mac, Integer tenantId);

    /**
     * Adds a mapping between a SwitchRoute and the PhysicalLinks making it up.
     *
     * @param route the switch route
     * @param physicalLinks the physical path
     */
    public void addRoute(final SwitchRoute route,
            final List<PhysicalLink> physicalLinks);

    // Access objects from dictionary given the key

    /**
     *
     * @param ip
     * @param tenantId
     * @return Physical IP address associated with virtual IP and tenant ID
     */
    public PhysicalIPAddress getPhysicalIP(OVXIPAddress ip, Integer tenantId)
            throws AddressMappingException;

    /**
     *
     * @param ip
     * @return Virtual IP address associated with physical IP
     */
    public OVXIPAddress getVirtualIP(PhysicalIPAddress ip)
            throws AddressMappingException;

    /**
     * Get the virtualSwitch which has been specified by the physicalSwitch and
     * tenantId.
     *
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     *
     * @return virtualSwitch A OVXSwitch object which represents a single switch
     *         in the OVXNetwork
     */
    public OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch,
            Integer tenantId) throws SwitchMappingException;

    /**
     * Gets the list of OVXLinks that are part of virtual network identified by
     * tenantId and which include the specified physicalLink.
     *
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     *
     * @return virtualLink A OVXLink object which represents a single link in
     *         the OVXNetwork
     */
    public List<OVXLink> getVirtualLinks(PhysicalLink physicalLink,
            Integer tenantId) throws LinkMappingException;

    /**
     * Get the physicalLinks that all make up a specified virtualLink. Return a
     * list of all the physicalLinks that make up the virtualLink.
     *
     * @param virtualLink
     *            An OVXLink object which represents a single link in the
     *            OVXNetwork
     *
     * @return physicalLinks A List of PhysicalLink objects which represent a
     *         single source and destination PhysicalPort and PhysicalSwitch
     */
    public List<PhysicalLink> getPhysicalLinks(OVXLink virtualLink)
            throws LinkMappingException;

    /**
     * Get the physicalSwitches that are contained in the virtualSwitch. for a
     * big switch this will be multiple physicalSwitches.
     *
     * @param virtualSwitch
     *            A OVXSwitch object representing a single switch in the virtual
     *            network
     *
     * @return physicalSwitches A List of PhysicalSwitch objects that are each
     *         part of the OVXSwitch specified
     */
    public List<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch)
            throws SwitchMappingException;

    /**
     * Gets the virtual network instance associated with the given tenant ID.
     *
     * @param tenantId the tenant ID
     * @return the virual network
     */
    public OVXNetwork getVirtualNetwork(Integer tenantId)
            throws NetworkMappingException;

    /**
     * Gets the tenant ID associated with the given MAC address.
     *
     * @param mac the MAC address
     * @return tenant ID associated with MAC address
     */
    public Integer getMAC(MACAddress mac) throws AddressMappingException;

    /**
     * Obtains an immutable copy of the list of virtual networks.
     *
     * @return - immutable list of virtual networks
     *
     */
    public Map<Integer, OVXNetwork> listVirtualNetworks();

    /**
     * Deletes the OVXNetwork object.
     *
     * @param OVXNetwork
     *            network This is object representing the virtual network
     */
    public void removeNetwork(OVXNetwork network);

    /**
     * Delete the OVXLink object.
     *
     * @param OVXLink
     *            virtualLink This is object representing the virtual link
     */
    public void removeVirtualLink(OVXLink virtualLink);

    /**
     * Delete the OVXSwitch object.
     *
     * @param OVXSwitch
     *            virtualSwitch This is object representing the virtual switch
     */
    public void removeVirtualSwitch(OVXSwitch virtualSwitch);

    /**
     * Delete all the IPs associated to a single virtual network.
     *
     * @param tenantId
     *            This is an Integer that represents a unique number for each
     *            virtualNetwork
     */
    public void removeVirtualIPs(int tenantId);

    /**
     * Removes the MAC address from the map.
     *
     * @param mac the MAC address
     * @param tenantId the tenant ID
     */
    public void removeMAC(MACAddress mac);

    /**
     * Gets the current path as a list of physical links for the given route.
     *
     * @param route the switch route
     * @return the path
     */
    public List<PhysicalLink> getRoute(SwitchRoute route)
            throws LinkMappingException;

    /**
     * Gets the switch routes that use the given physical link in
     * the specified virtual network.
     *
     * @param physicalLink the physical link
     * @param tenantId the tenant ID
     * @return The routes associated with the supplied PhysicalLink
     */
    public Set<SwitchRoute> getSwitchRoutes(PhysicalLink physicalLink,
            Integer tenantId) throws LinkMappingException;

    /**
     * Removes a SwitchRoute from the map.
     *
     * @param route the switch route
     */
    public void removeRoute(SwitchRoute route);

    /**
     * Removes a PhysicalLink from a Mappable, including mappings to OVXLinks
     * and SwitchRoutes.
     *
     * @param physicalLink the physical link
     */
    public void removePhysicalLink(PhysicalLink physicalLink);

    /**
     * Removes a PhysicalSwitch from Mappable mappings.
     *
     * @param physicalSwitch the physical switch
     */
    public void removePhysicalSwitch(PhysicalSwitch physicalSwitch);

    /*
     * Below: helper functions needed to avoid using error exception for flow
     * control
     */
    /**
     * @param vip
     *            Virtual IP address
     * @param tenantId
     *            the ID representing a virtual network.
     * @return true if a PhysicalIPAddress exists in this map
     */
    public boolean hasPhysicalIP(OVXIPAddress vip, Integer tenantId);

    /**
     * @param ip
     *            The physical IP address
     * @return true if a mapping exists
     */
    public boolean hasVirtualIP(PhysicalIPAddress ip);

    /**
     * Checks if the MAC address exists in the map.
     *
     * @param mac the MAC address
     * @return true if the MAC address exists in this map
     */
    public boolean hasMAC(MACAddress mac);

    /**
     * @param physicalLink
     *            the PhysicalLink
     * @param tenantId
     *            the ID representing a virtual network.
     * @return true if a PhysicalLink maps to any SwitchRoutes
     */
    public boolean hasSwitchRoutes(final PhysicalLink physicalLink,
            final Integer tenantId);

    /**
     * @param physicalLink
     *            the PhysicalLink
     * @param tenantId
     *            the ID representing a virtual network.
     * @return true if a PhysicalLink maps to any OVXLinks
     */
    public boolean hasOVXLinks(final PhysicalLink physicalLink,
            final Integer tenantId);

    /**
     * @param psw
     *            the PhysicalSwitch mapped to the OVXSwitch we want to check
     *            for
     * @param tid
     *            TenantID of OVXSwitch
     * @return true if the OVXSwitch exists
     */
    public boolean hasVirtualSwitch(PhysicalSwitch psw, int tid);

    /**
     * Sets the linkid back to the original linkid for a physical link that
     * previously existed and has gone away and come back.
     *
     * @param link
     *            - the new link that has just been detected,
     * @return a linkId if this link was known, otherwise null
     */
    public void knownLink(PhysicalLink link);

}
