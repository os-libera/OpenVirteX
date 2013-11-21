/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.routing;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.PortMappingException;

public interface Routable {

	public LinkedList<PhysicalLink> computePath(OVXPort srcPort, OVXPort dstPort);

	/**
	 * @param virtualSwitch
	 *            The virtual big switch
	 * @param ingress
	 *            The ingress port on the big switch
	 * @param egress
	 *            The egress port on the big switch
	 * @return A list of links (tentative) representing the route across the big
	 *         switch
	 */
	public SwitchRoute getRoute(OVXBigSwitch vSwitch, OVXPort srcPort,
			OVXPort dstPort);

	/**
	 * @return The name of the routing policy
	 */
	public String getName();

	/**
	 * Sets end-points to a route 
	 * @param ovxLink The link representing the end-points of a route
	 * @throws PortMappingException
	 */
	public void setLinkPath(OVXLink ovxLink) throws PortMappingException;
}
