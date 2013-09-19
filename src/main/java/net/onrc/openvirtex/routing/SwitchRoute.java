package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.List;

import net.onrc.openvirtex.elements.link.PhysicalLink;

/**
 * Route within a Big Switch abstraction
 * 
 */
public class SwitchRoute {

	/** unique route identifier */
	int routeId;

	/** DPID of parent virtual switch */
	long dpid;

	/** list of links making up route */
	ArrayList<PhysicalLink> routeList;

	public SwitchRoute(final long dpid, final int routeid) {
		this.dpid = dpid;
		this.routeId = routeid;
		this.routeList = new ArrayList<PhysicalLink>();
	}

	public void setRouteId(final int routeid) {
		this.routeId = routeid;
	}

	/**
	 * @return the ID of this route
	 */
	public int getRouteId() {
		return this.routeId;
	}

	public void setSwitchId(final long dpid) {
		this.dpid = dpid;
	}

	/**
	 * @return the DPID of the virtual switch
	 */
	public long getSwitchId() {
		return this.dpid;
	}

	/**
	 * associates this route with a set of links
	 * 
	 * @param path
	 */
	public void addRoute(final List<PhysicalLink> path) {
		for (final PhysicalLink hop : path) {
			this.routeList.add(hop);
		}
	}

	/**
	 * @return the links in this route
	 */
	public ArrayList<PhysicalLink> getRoute() {
		return this.routeList;
	}

	@Override
	public String toString() {
		String sroute = "routeId: " + this.routeId + " dpid: " + this.dpid
				+ " route: ";
		for (final PhysicalLink pl : this.routeList) {
			sroute += pl.toString() + " ";
		}
		return sroute;
	}
}
