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
     * @param virtualIP
     */
    void addIPMapping(PhysicalIPAddress physicalIP, OVXIPAddress virtualIP);

    /**
     * create the mapping between virtual switch to physical switch and from
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     */
    void addSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch);

    /**
     * create the mapping between the virtual link to physical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     * @param virtualLink
     */
    void addLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink);

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork      
     */
    void addNetworkMapping(OVXNetwork virtualNetwork);

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
    List<PhysicalLink> getPhysicalLinks(OVXLink virtualLink);

    /**
     * get the physicalSwitches that are contained in the virtualSwitch. for
     * a big switch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    List<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /**
     * use the tenantId to return the OVXNetwork object.
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    OVXNetwork getVirtualNetwork(Integer tenantId);

}
