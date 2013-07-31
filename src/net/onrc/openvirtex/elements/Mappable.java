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
     * sets up the mapping from the physical link to the virtualLinks which
     * contain the given physical link
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
     * maps the virtual link to the physical links that it contains
     * 
     * @param virtualLink
     * @param physicalLinks
     * @return success
     */
    boolean addVirtualLinkMapping(OVXLink virtualLink, PhysicalLink physicalLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     * 
     * @return success
     */
    boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork);

    // Access objects from dictionary given the key

    /**
     * get the virtualSwitch which has been specified by the physicalSwitch
     * and tenantId
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId);
    
    /**
     * get the virtualLink which has been specified by the physicalLink and
     * the tenantId. This function will return a list of virtualLinks all of
     * which contain the specified physicalLink in the tenantId.
     * 
     * @param physicalLink
     * 
     * @return virtualLink
     */
    OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId);

    /**
     * get the physicalLinks that all make up a specified virtualLink.
     * Return a list of all the physicalLinks that make up the virtualLink
     * 
     * @param virtualLink
     * 
     * @return physicalLinks
     */
    ArrayList<PhysicalLink> getPhysicalLinks(OVXLink virtualLink);

    /**
     * get the physicalSwitches that are contained in the virtualSwitch. for
     * a bigswitch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /**
     * using the tenantId return the OVXNetwork object which is reffered to by
     * the specified tenantId.
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    OVXNetwork getVirtualNetwork(Integer tenantId);

}
