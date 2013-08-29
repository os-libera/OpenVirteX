/**
 * 
 */
package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public class ShortestPath implements Routable {

	@Override
	public LinkedList<PhysicalLink> computePath(final OVXPort srcPort,
			final OVXPort dstPort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
        public ArrayList<PhysicalLink> getRoute(OVXBigSwitch vSwitch,
                OVXPort srcPort, OVXPort dstPort) {
	    // TODO Auto-generated method stub
	    return null;
        }
	
	public String getName() {
		return "shortest path";
	}
}
