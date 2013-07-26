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
    /*
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     */
    boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch,
	    OVXSwitch virtualSwitch);

    /*
     * keep track of each physical port and all the virtual ports that
     * use the physical port mentioned
     */
    boolean addPhysicalPortMapping(PhysicalPort physicalPort,
	    OVXPort virtualPort);

    /*
     * in order to get the list of virtual links that all have
     * used a specific physical link
     */
    boolean addPhysicalLinkMapping(PhysicalLink physicalLink,
	    OVXLink virtualLink);

    /*
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     */
    boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch,
	    PhysicalSwitch physicalSwitch);

    /*
     * maps the OVXPort object to the physical Port that it refers to
     */
    boolean addVirtualPortMapping(OVXPort virtualPort, PhysicalPort physicalPort);

    /*
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     */
    boolean addVirtualLinkMapping(OVXLink virtualLink,
	    List<PhysicalLink> physicalLinks);

    /*
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     */
    boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork);

    // Access objects from dictionary given the key

    /*
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     */
    ArrayList<OVXSwitch> getVirtualSwitches(PhysicalSwitch physicalSwitch);

    /*
     * keep track of each physical port and all the virtual ports that
     * use the physical port mentioned
     */
    ArrayList<OVXPort> getVirtualPorts(PhysicalPort physicalPort);

    /*
     * in order to get the list of physical links that all have
     * used a specific physical link
     */
    ArrayList<PhysicalLink> getPhysicalLinks(OVXMap virtualLink);

    /*
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     */
    ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);

    /*
     * maps the OVXPort object to the physical Port that it refers to
     */
    ArrayList<PhysicalPort> getPhysicalPorts(OVXPort virtualPort);

    /*
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     */
    ArrayList<OVXLink> getVirtualLinks(PhysicalLink physicalLink);

    /*
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     */
    OVXNetwork getVirtualNetwork(int tenantId);

}
