package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

public class ManualRoute implements Routable {

    @Override
    public LinkedList<PhysicalLink> computePath(OVXPort srcPort, OVXPort dstPort) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public ArrayList<PhysicalLink> getRoute(OVXBigSwitch vSwitch,
	    OVXPort srcPort, OVXPort dstPort) {
	//return route that was set manually 
	return vSwitch.getRouteMap().get(srcPort).get(dstPort).getRoute();
    }

    public String getName() {
	return "manual";
    }
}
