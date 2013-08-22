package net.onrc.openvirtex.routing;

import java.util.ArrayList;

import net.onrc.openvirtex.elements.link.PhysicalLink;

/**
 * Route within a Big Switch abstraction
 * 
 */
public class SwitchRoute {
    
    /** unique route identifier*/
    int routeId;
    
    /** DPID of virtual switch */
    long dpid;
    
    /** list of links making up route */
    ArrayList<PhysicalLink> routeList;
    
    public SwitchRoute(long dpid, int routeid) {
	this.dpid = dpid;
	this.routeId = routeid;
	this.routeList = new ArrayList<PhysicalLink>();
    }
    
    public void setRouteId(int routeid) {
	this.routeId = routeid;
    }
    
    /**
     * @return the ID of this route
     */
    public int getRouteId() {
	return this.routeId;
    }
    
    /**
     * @return the DPID of the virtual switch 
     */
    public long getSwitchId() {
	return this.dpid;
    }
    
    /**
     * @return the links in this route
     */
    public ArrayList<PhysicalLink> getRoute() {
	return this.routeList;
    }
}
