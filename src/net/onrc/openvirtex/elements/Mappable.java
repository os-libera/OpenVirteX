/**
 * 
 */
package net.onrc.openvirtex.elements;

import java.util.ArrayList;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;

public interface Mappable {
    // ADD objects to dictionary
     
    /**
     * getInstance will get the instance of the class and if this already exists
     * then the existing object will be returned. This is a singleton class.
     * 
     * @return OVXMap
     */
    Mappable getInstance();
    
    /**
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     * @return success
     */
    boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch,
	    OVXSwitch virtualSwitch);

    /**
     * in order to get the list of virtual links that all have
     * used a specific physical link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    boolean addPhysicalLinkMapping(PhysicalLink physicalLink,
	    OVXLink virtualLink);

    /**
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     * @param physicalSwitch
     * 
     * @return success
     */
    boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch,
	    PhysicalSwitch physicalSwitch);

    /**
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     * 
     * @param virtualLink
     * @param physicalLinks
     * @return success
     */
    boolean addVirtualLinkMapping(OVXLink virtualLink, PhysicalLink physicalLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     * 
     * @param virtualNetwork
     * 
     * @return success
     */
    boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork);

    // Access objects from dictionary given the key

    /**
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId);
    
    /**
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     * 
     * @param physicalLink
     * 
     * @return virtualLinks
     */
    OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId);

    /**
     * in order to get the list of physical links that all have
     * used a specific physical link
     * 
     * @param virtualLink
     * 
     * @return physicalLinks
     */
    ArrayList<PhysicalLink> getPhysicalLinks(OVXMap virtualLink);

    /**
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    OVXNetwork getVirtualNetwork(int tenantId);

}
