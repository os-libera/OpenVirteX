/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.routing.ManualRoute;
import net.onrc.openvirtex.routing.Routable;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

/**
 * The Class OVXBigSwitch.
 * 
 */

public class OVXBigSwitch extends OVXSwitch {

	private static Logger log = LogManager.getLogger(OVXBigSwitch.class
			.getName());

	/** The alg. */
	private RoutingAlgorithms alg;

	/** The routing mechanism */
	private Routable routing;

	/** The calculated routes */
	private final HashMap<OVXPort, HashMap<OVXPort, SwitchRoute>> routeMap;

	public OVXBigSwitch(final long switchId, final int tenantId) {
		super(switchId, tenantId);
		this.alg = RoutingAlgorithms.NONE;
		this.routing = new ManualRoute();
		this.routeMap = new HashMap<OVXPort, HashMap<OVXPort, SwitchRoute>>();
	}

	/**
	 * Gets the alg.
	 * 
	 * @return the alg
	 */
	public RoutingAlgorithms getAlg() {
		return this.alg;
	}

	/**
	 * Sets the alg.
	 * 
	 * @param alg
	 *            the new alg
	 */
	public void setAlg(final RoutingAlgorithms alg) {
		this.alg = alg;
		this.routing = alg.getRoutable();
	}

	/**
	 * @return The routable for this Big Switch
	 */
	public Routable getRoutable() {
		return this.routing;
	}

	/**
	 * @param srcPort
	 *            the ingress port on the Big Switch
	 * @param dstPort
	 *            the egress port on the Big Switch
	 * @return The route 
	 */
	public SwitchRoute getRoute(final OVXPort srcPort, final OVXPort dstPort) {
		return this.routing.getRoute(this, srcPort, dstPort);
	}

	/**
	 * Fetch all routes associated with a specific port, assuming that the 
	 * routes are duplex. Does not account for the use of backups for now... 
	 * 
	 * @param port
	 * @return
	 */
	public Set<SwitchRoute> getRoutebyPort(final OVXPort port) {
		Set<SwitchRoute> routes = new HashSet<SwitchRoute>();
		for (OVXPort vport : this.portMap.values()) {
			if (vport.equals(port)) {
				continue;
			}
			SwitchRoute rt = getRoute(port, vport);
			if (rt != null) {
				SwitchRoute revrt = getRoute(vport, port);    
			    	routes.add(rt);
			    	routes.add(revrt);
			}
		}
		return routes;
	}
	
	public HashMap<OVXPort, HashMap<OVXPort, SwitchRoute>> getRouteMap() {
		return this.routeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.onrc.openvirtex.elements.datapath.Switch#removePort(short)
	 */
	@Override
	public boolean removePort(final Short portNumber) {
		if (!this.portMap.containsKey(portNumber)) {
			return false;
		} else {
			this.portMap.remove(portNumber);
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.
	 * OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
	 */
	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		// TODO Truncate the message for the ctrl to the missSetLenght value
		if (this.isConnected  && this.isActive) {
			this.channel.write(Collections.singletonList(msg));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
	 * .OFMessage)
	 */
	@Override
	public void handleIO(final OFMessage msg) {
		try {
			((Devirtualizable) msg).devirtualize(this);
		} catch (final ClassCastException e) {
			OVXBigSwitch.log.error("Received illegal message : " + msg);
		}

	}

	@Override
	public boolean boot() {
		return super.boot();
		// TODO: Start the internal routing protocol
	}

	/**
	 * Gets the port.
	 * 
	 * @param portNumber
	 *            the port number
	 * @return the port instance
	 */
	@Override
	public OVXPort getPort(final Short portNumber) {
		return this.portMap.get(portNumber);
	};

	@Override
	public String toString() {
		return "SWITCH:\n- switchId: " + this.switchId + "\n- switchName: "
				+ this.switchName + "\n- isConnected: " + this.isConnected
				+ "\n- tenantId: " + this.tenantId + "\n- missSendLenght: "
				+ this.missSendLen + "\n- isActive: " + this.isActive
				+ "\n- capabilities: "
				+ this.capabilities.getOVXSwitchCapabilities()
				+ "\n- algorithm: " + this.alg.getValue();
	}

	@Override
	public void sendSouth(final OFMessage msg, final OVXPort inPort) {
		if (inPort == null) {
			/* TODO for some OFTypes, we can recover an inport. */    
	    		return;
		}
		final PhysicalSwitch sw = inPort.getPhysicalPort().getParentSwitch();
		sw.sendMsg(msg, this);
	}

	/**
	 * Adds a path between two edge ports on the big switch
	 * 
	 * @param ingress
	 * @param egress
	 * @param path
	 *            list of links
	 * @param revpath
	 *            the corresponding reverse path from egress to ingress
	 * @return the route ID of the new route
	 */
	public int createRoute(final OVXPort ingress, final OVXPort egress,
			final List<PhysicalLink> path, final List<PhysicalLink> revpath) {
		final int routeId = this.map.getVirtualNetwork(this.tenantId)
				.getLinkCounter().getAndIncrement();
		final SwitchRoute rtEntry = new SwitchRoute(
				ingress, egress, this.switchId, routeId, this.tenantId);
		final SwitchRoute revRtEntry = new SwitchRoute(
				egress, ingress, this.switchId, routeId, this.tenantId);
		this.map.addRoute(rtEntry, path);
		this.map.addRoute(revRtEntry, revpath);

		this.addToRouteMap(ingress, egress, rtEntry);
		this.addToRouteMap(egress, ingress, revRtEntry);

		OVXBigSwitch.log.info("Added route {}", rtEntry);
		return routeId;
	}

	private void addToRouteMap(final OVXPort in, final OVXPort out,
			final SwitchRoute entry) {
	    	HashMap<OVXPort, SwitchRoute> rtmap = this.routeMap.get(in);
		if (rtmap == null) {
			rtmap = new HashMap<OVXPort, SwitchRoute>();
			this.routeMap.put(in, rtmap);
		}
		rtmap.put(out, entry);
	}

	@Override
	public int translate(final OFMessage ofm, final OVXPort inPort) {
		if (inPort == null) {
			// don't know the PhysicalSwitch, for now return original XID.
			return ofm.getXid();
		} else {
			// we know the PhysicalSwitch
			final PhysicalSwitch psw = inPort.getPhysicalPort()
					.getParentSwitch();
			return psw.translate(ofm, this);
		}
	}
}
