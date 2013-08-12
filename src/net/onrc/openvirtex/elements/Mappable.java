/**
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
    void addIP(PhysicalIPAddress physicalIP, OVXIPAddress virtualIP);

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
     * create the mapping between virtual switch to physical switch and from
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     *            Refers to the PhysicalSwitch from the PhysicalNetwork
     * @param virtualSwitch
     *            Has type OVXSwitch and this switch is specific to a tenantId
     * 
     */
    void addSwitch(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch);

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
     * create the mapping between the virtual link to physical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     *            Refers to the PhysicalLink in the PhysicalNetwork
     * @param virtualLink
     *            Refers to the OVXLink which consists of many PhysicalLinks and
     *            specific to a tenantId
     */
    void addLink(PhysicalLink physicalLink, OVXLink virtualLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     *            An OVXNetwork object which keeps track of all the elements in
     *            the Virtual network
     */
    void addNetwork(OVXNetwork virtualNetwork);

    // Access objects from dictionary given the key

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
    OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId);

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
    OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId);

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
    List<PhysicalLink> getPhysicalLinks(OVXLink virtualLink);

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
    List<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

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
    OVXNetwork getVirtualNetwork(Integer tenantId);

}
