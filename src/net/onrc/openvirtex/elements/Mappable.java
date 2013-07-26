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

/**
 * @author Karthik Jagadeesh
 *
 */
public interface Mappable {
    // ADD objects to dictionary
    
    boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch);
    
    boolean addPhysicalPortMapping(PhysicalPort physicalPort, OVXPort virtualPort);
    
    boolean addPhysicalLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink);
    
    boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch, PhysicalSwitch physicalSwitch);
    
    boolean addVirtualPortMapping(OVXPort virtualPort, PhysicalPort physicalPort);
    
    boolean addVirtualLinkMapping(OVXLink virtualLink, List <PhysicalLink> physicalLinks);
    
    boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork);
    
    //Access objects from dictionary given the key
    
    ArrayList<OVXSwitch> getVirtualSwitches(PhysicalSwitch physicalSwitch);
    
    ArrayList<OVXPort> getVirtualPorts(PhysicalPort physicalPort);
    
    ArrayList<PhysicalLink> getPhysicalLinks(OVXMap virtualLink);
    
    ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch);
    
    ArrayList<PhysicalPort> getPhysicalPorts(OVXPort virtualPort);
    
    ArrayList<OVXLink> getVirtualLinks(PhysicalLink physicalLink);
    
    OVXNetwork getVirtualNetwork(int tenantId);
    
}
