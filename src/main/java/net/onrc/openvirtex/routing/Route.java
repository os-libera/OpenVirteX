/**
 * 
 */
package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public abstract class Route implements Routable {
	protected OVXMap map;

	public abstract LinkedList<PhysicalLink> computePath(OVXPort srcPort,
			OVXPort dstPort);
	
	public abstract ArrayList<PhysicalLink> getRoute(OVXBigSwitch vSwitch, 
		    OVXPort srcPort, OVXPort dstPort);
}
