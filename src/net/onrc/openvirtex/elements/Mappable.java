/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.elements;

import java.util.List;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
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
     * function takes
     * in a list of physicalSwitches and adds to the OVXMap indexed by the
     * virtualSwitch.
     * 
     * @param physicalSwitches
     * @param virtualSwitch
     */
    public void addSwitches(final List<PhysicalSwitch> physicalSwitches,
	    final OVXSwitch virtualSwitch);

    /**
     * Create the mapping between PhysicalLinks and a VirtualLink. This function
     * takes in a
     * list of physicalLinks rather than an individual physicalLink and adds the
     * list
     * to the OVXmap.
     * 
     * @param physicalLinks
     * @param virtualLink
     */
    public void addLinks(final List<PhysicalLink> physicalLinks,
	    final OVXLink virtualLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     *            An OVXNetwork object which keeps track of all the elements in
     *            the Virtual network
     */
    public void addNetwork(OVXNetwork virtualNetwork);

    /**
     * This function creates the map between the MAC and tenantId
     * 
     * @param mac
     * @param tenantId
     */
    public void addMAC(MACAddress mac, Integer tenantId);
    
    // Access objects from dictionary given the key

    /**
     * 
     * @param ip
     * @param tenantId
     * @return
     * 		Physical IP address associated with virtual IP and tenant ID
     */
    public PhysicalIPAddress getPhysicalIP(OVXIPAddress ip, Integer tenantId);
    
    /**
     * 
     * @param ip
     * @return
     * 		Virtual IP address associated with physical IP
     */
    public OVXIPAddress getVirtualIP(PhysicalIPAddress ip);
    
    /**
     * get the virtualSwitch which has been specified by the physicalSwitch
     * and tenantId
     * 
     * @param physicalSwitch
     *            A PhysicalSwitch object is a single switch in the
     *            PhysicalNetwork
     * 
     * @return virtualSwitch A OVXSwitch object which represents a single switch
     *         in the OVXNetwork
     */
    public OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId);

    /**
     * get the virtualLink which has been specified by the physicalLink and
     * the tenantId. This function will return a list of virtualLinks all of
     * which contain the specified physicalLink in the tenantId.
     * 
     * @param physicalLink
     *            A PhysicalLink object which represent a single source and
     *            destination PhysicalPort and PhysicalSwitch
     * 
     * @return virtualLink A OVXLink object which represents a single link in
     *         the OVXNetwork
     */
    public OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId);

    /**
     * get the physicalLinks that all make up a specified virtualLink.
     * Return a list of all the physicalLinks that make up the virtualLink
     * 
     * @param virtualLink
     *            An OVXLink object which represents a single link in the
     *            OVXNetwork
     * 
     * @return physicalLinks A List of PhysicalLink objects which represent a
     *         single source and destination PhysicalPort and PhysicalSwitch
     */
    public List<PhysicalLink> getPhysicalLinks(OVXLink virtualLink);

    /**
     * get the physicalSwitches that are contained in the virtualSwitch. for
     * a big switch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     *            A OVXSwitch object representing a single switch in the virtual
     *            network
     * 
     * @return physicalSwitches A List of PhysicalSwitch objects that are each
     *         part of the OVXSwitch specified
     */
    public List<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /**
     * use the tenantId to return the OVXNetwork object.
     * 
     * @param tenantId
     *            This is an Integer that represents a unique number for each
     *            virtualNetwork
     * 
     * @return virtualNetwork A OVXNetwork object that represents all the
     *         information related to a virtual network
     */
    public OVXNetwork getVirtualNetwork(Integer tenantId);

    /**
     * 
     * @param mac
     * @return
     * 		tenantId associated with MAC address
     */
    public Integer getMAC(MACAddress mac);
}
