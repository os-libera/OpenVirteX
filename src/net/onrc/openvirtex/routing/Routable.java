package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

public interface Routable {
    
    public LinkedList<PhysicalLink> computePath(OVXPort srcPort,
		OVXPort dstPort);
    
    /**
     * @param virtualSwitch The virtual big switch 
     * @param ingress The ingress port on the big switch
     * @param egress The egress port on the big switch
     * @return A list of links (tentative) representing the route across the big switch
     */
    public ArrayList<PhysicalLink> getRoute(OVXBigSwitch vSwitch, 
	    OVXPort srcPort, OVXPort dstPort);
    
    public String getName();
}
