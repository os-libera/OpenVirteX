/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class ShortestPath. This class implements the Dijstra Algorithm to compute the shortest path 
 * between two physical switches based on the nominal throughput of the link.
 */
public class ShortestPath implements Routable {
    
    /** The log. */
    private static Logger log = LogManager.getLogger(ShortestPath.class
	                              .getName());

    /**
     * The Class Predecessor. This class store the info related to a physicalSwitch predecessor.
     * In particular, each predecessor is a physicalSwitch, but also the best physicalLink between 
     * the switches is stored. This solution allow to support multiple physical links between two 
     * physical switches
     */
    public class Predecessor {
	
	/** The sw. */
	private final PhysicalSwitch sw;
	
	/** The link. */
	private final PhysicalLink   link;

	/**
	 * Instantiates a new predecessor.
	 *
	 * @param sw the sw
	 * @param link the link
	 */
	public Predecessor(final PhysicalSwitch sw, final PhysicalLink link) {
	    this.sw = sw;
	    this.link = link;
	}

	/**
	 * Gets the switch.
	 *
	 * @return the switch
	 */
	public PhysicalSwitch getSwitch() {
	    return this.sw;
	}

	/**
	 * Gets the link.
	 *
	 * @return the link
	 */
	public PhysicalLink getLink() {
	    return this.link;
	}
    }

    /** The edges. */
    private final List<PhysicalLink>         edges;
    
    /** The settled nodes. */
    private Set<PhysicalSwitch>              settledNodes;
    
    /** The un settled nodes. */
    private Set<PhysicalSwitch>              unSettledNodes;
    
    /** The predecessors. */
    private Map<PhysicalSwitch, Predecessor> predecessors;
    
    /** The distance. */
    private Map<PhysicalSwitch, Integer>     distance;

    
    /**
     * Instantiates a new shortest path. Get an immutable COPY of the physical links 
     * from the physical network and stores it in a local list
     */
    public ShortestPath() {
	this.edges = new ArrayList<PhysicalLink>(PhysicalNetwork.getInstance()
	        .getLinks());
    }

    /**
     * Compute all the path between a source switch and all the switches in the network
     *
     * @param source the source
     */
    /*
     * TODO: The alg is not optimal because it recompute all the paths from the source every 
     * time we call computePath. A solution is to store this info. Evaluate pros/cons
     */
    public void execute(final PhysicalSwitch source) {
	this.settledNodes = new HashSet<PhysicalSwitch>();
	this.unSettledNodes = new HashSet<PhysicalSwitch>();
	this.distance = new HashMap<PhysicalSwitch, Integer>();
	this.predecessors = new HashMap<PhysicalSwitch, Predecessor>();
	this.distance.put(source, 0);
	this.unSettledNodes.add(source);
	while (this.unSettledNodes.size() > 0) {
	    final PhysicalSwitch node = this.getMinimum(this.unSettledNodes);
	    this.settledNodes.add(node);
	    this.unSettledNodes.remove(node);
	    this.findMinimalDistances(node);
	}
    }

    /**
     * Find minimal distances.
     *
     * @param node the node
     */
    private void findMinimalDistances(final PhysicalSwitch node) {
	final List<PhysicalSwitch> adjacentNodes = this.getNeighbors(node);
	for (final PhysicalSwitch target : adjacentNodes) {
	    if (this.getShortestDistance(target) > this
		    .getShortestDistance(node)
		    + this.getBestLink(node, target).getMetric()) {
		final PhysicalLink bestLink = this.getBestLink(node, target);
		this.distance.put(target, this.getShortestDistance(node)
		        + bestLink.getMetric());
		this.predecessors.put(target, new Predecessor(node, bestLink));
		this.unSettledNodes.add(target);
	    }
	}
    }

    /**
     * Gets the best link between two switches. Uses the link metric to evaluate 
     * the link with the highest throughput (e.g. the lowest metric)
     *
     * @param node the source node
     * @param target the destination node
     * @return the best link
     */
    private PhysicalLink getBestLink(final PhysicalSwitch node,
	    final PhysicalSwitch target) {
	PhysicalLink bestLink = null;
	for (final PhysicalLink edge : this.edges) {
	    if (edge.getSrcSwitch().equals(node)
		    && edge.getDstSwitch().equals(target)) {
		if (bestLink == null || bestLink.getMetric() > edge.getMetric()) {
		    bestLink = edge;
		}
	    }
	}
	return bestLink;	    
    }

    /**
     * Gets the switch neighbors in the physical network
     *
     * @param node the node
     * @return the neighbors list
     */
    private List<PhysicalSwitch> getNeighbors(final PhysicalSwitch node) {
	final List<PhysicalSwitch> neighbors = new ArrayList<PhysicalSwitch>();
	for (final PhysicalLink edge : this.edges) {
	    if (edge.getSrcSwitch().equals(node)
		    && !this.isSettled(edge.getDstSwitch())) {
		neighbors.add(edge.getDstSwitch());
	    }
	}
	return neighbors;
    }

    /**
     * Return the physical switch with the lowest metric
     *
     * @param list of physical neighbours
     * @return the closest switch
     */
    private PhysicalSwitch getMinimum(final Set<PhysicalSwitch> vertexes) {
	PhysicalSwitch minimum = null;
	for (final PhysicalSwitch vertex : vertexes) {
	    if (minimum == null) {
		minimum = vertex;
	    } else {
		if (this.getShortestDistance(vertex) < this
		        .getShortestDistance(minimum)) {
		    minimum = vertex;
		}
	    }
	}
	return minimum;
    }

    /**
     * Checks if the physical switch is the settle list
     *
     * @param vertex the physical switch
     * @return true, if is settled
     */
    private boolean isSettled(final PhysicalSwitch vertex) {
	return this.settledNodes.contains(vertex);
    }

    /**
     * Gets the shortest distance.
     *
     * @param destination the destination
     * @return the shortest distance
     */
    private int getShortestDistance(final PhysicalSwitch destination) {
	final Integer d = this.distance.get(destination);
	if (d == null) {
	    return Integer.MAX_VALUE;
	} else {
	    return d;
	}
    }

    /**
     * Gets the list of physical path between to virtual ports
     * 
     * @param srcPort the virtual source port
     * @param dstPort the virtual destination port
     * 
     * @return path list of physical links between to physical switches
     */
    @Override
    public LinkedList<PhysicalLink> computePath(final OVXPort srcPort,
	    final OVXPort dstPort) {
	final LinkedList<PhysicalLink> path = new LinkedList<PhysicalLink>();
	if (srcPort.getPhysicalPort().getParentSwitch() != dstPort.getPhysicalPort().getParentSwitch()) {
	    final LinkedList<PhysicalLink> tmpPath = new LinkedList<PhysicalLink>();
	    this.execute(srcPort.getPhysicalPort().getParentSwitch());
	    PhysicalSwitch step = dstPort.getPhysicalPort().getParentSwitch();
	    // check if a path exists
	    if (this.predecessors.get(step) == null) {
		return null;
	    }
	    while (this.predecessors.get(step) != null) {
		tmpPath.add(this.predecessors.get(step).getLink());
		step = this.predecessors.get(step).getSwitch();
	    }
	    // Put it into the correct order
	    Collections.reverse(tmpPath);
	    for (PhysicalLink link : tmpPath) 
		path.add(PhysicalNetwork.getInstance().getLink(link.getSrcPort(), link.getDstPort()));
	}
	return path;
    }

    /**
     * Gets the route element that represent a path between to virtual ports.
     * If the route has been already compute, returns the instance, otherwise
     * it call the dijstra algorithm to compute it.
     * 
     * @param vSwitch the virtual big switch
     * @param srcPort the virtual source port
     * @param dstPort the virtual destination port
     * 
     * @return switch route
     */
    @Override
    public SwitchRoute getRoute(final OVXBigSwitch vSwitch,
	    final OVXPort srcPort, final OVXPort dstPort) {
	final HashMap<OVXPort, HashMap<OVXPort, SwitchRoute>> routeMap = vSwitch
	        .getRouteMap();
	HashMap<OVXPort, SwitchRoute> portRouteMap = null;
	SwitchRoute route = null;
	if ((portRouteMap = routeMap.get(srcPort)) != null) {
	    if ((route = portRouteMap.get(dstPort)) != null) {
		return route;
	    }
	}

	final LinkedList<PhysicalLink> path = this
	        .computePath(srcPort, dstPort);
	final LinkedList<PhysicalLink> revpath = new LinkedList<PhysicalLink>();
	for (final PhysicalLink link : path) {
	    final PhysicalLink revhop = PhysicalNetwork.getInstance().getLink(
		    link.getDstPort(), link.getSrcPort());
	    revpath.add(revhop);
	}
	try {
	    Collections.reverse(revpath);
	    vSwitch.createRoute(srcPort, dstPort, path, revpath);
	} catch (final IndexOutOfBoundException e) {
	    ShortestPath.log
		    .error("Impossible to create the virtual switch route for for big-switch {} "
		            + "between ports ({},{}), too many routes in this virtual switch",
		            vSwitch.getSwitchName(), srcPort.getPortNumber(),
		            dstPort.getPortNumber());
	}
	return routeMap.get(srcPort).get(dstPort);
    }

    /* (non-Javadoc)
     * @see net.onrc.openvirtex.routing.Routable#getName()
     */
    @Override
    public String getName() {
	return "shortest path";
    }
}
