/**
 * 
 */
package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

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
     * keep track of each physical port and all the virtual ports that
     * use the physical port mentioned
     * 
     * @param physicalPort
     * @param virtualPort
     * 
     * @return virtualPort
     */
    boolean addPhysicalPortMapping(PhysicalPort physicalPort,
	    OVXPort virtualPort);

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
     * maps the OVXPort object to the physical Port that it refers to
     * 
     * @param virtualPort
     * @param physicalPort
     * 
     * @return success
     */
    boolean addVirtualPortMapping(OVXPort virtualPort, PhysicalPort physicalPort);

    /**
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     * 
     * @param virtualLink
     * @param physicalLinks
     * @return success
     */
    boolean addVirtualLinkMapping(OVXLink virtualLink,
	    List<PhysicalLink> physicalLinks);

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
    ArrayList<OVXSwitch> getVirtualSwitches(PhysicalSwitch physicalSwitch);

    /**
     * keep track of each physical port and all the virtual ports that
     * use the physical port mentioned
     * 
     * @param physicalPort
     * 
     * @return virtualPorts
     */
    ArrayList<OVXPort> getVirtualPorts(PhysicalPort physicalPort);
    
    /**
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     * 
     * @param physicalLink
     * 
     * @return virtualLinks
     */
    ArrayList<OVXLink> getVirtualLinks(PhysicalLink physicalLink);

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
     * maps the OVXPort object to the physical Port that it refers to
     * 
     * @param virtualPort
     * 
     * @return physicalPorts
     */
    ArrayList<PhysicalPort> getPhysicalPorts(OVXPort virtualPort);

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
