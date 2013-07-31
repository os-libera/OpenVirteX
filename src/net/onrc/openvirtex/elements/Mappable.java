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
     * create the mapping between virtual switch to physical switch and from 
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     * @return success
     */
    boolean addSwitchMapping(PhysicalSwitch physicalSwitch,
	    OVXSwitch virtualSwitch);

    /**
     * create the mapping between the virtual link to physical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    boolean addLinkMapping(PhysicalLink physicalLink,
	    OVXLink virtualLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     * 
     * @return success
     */
    boolean addNetworkMapping(OVXNetwork virtualNetwork);

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
     * a big switch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /**
     * use the tenantId to return the OVXNetwork object.
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    OVXNetwork getVirtualNetwork(Integer tenantId);

}
