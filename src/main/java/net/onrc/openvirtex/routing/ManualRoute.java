/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.routing;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

public class ManualRoute implements Routable {

	/** The log. */
	private static Logger log = LogManager.getLogger(ManualRoute.class
			.getName());
	
	@Override
	public LinkedList<PhysicalLink> computePath(final OVXPort srcPort,
			final OVXPort dstPort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SwitchRoute getRoute(final OVXBigSwitch vSwitch,
			final OVXPort srcPort, final OVXPort dstPort) {
		// return route that was set manually
		// TODO : throw 'route not initialized' type of exception if null 
		return vSwitch.getRouteMap().get(srcPort).get(dstPort);
	}

	@Override
	public String getName() {
		return "manual";
	}

	@Override
	public void setLinkPath(OVXLink ovxLink) {
		log.warn("The manual routing should never call the method [setLinkPath(OVXLink)]. "
				+ "To define a manual path for the virtual link, use the API call [SetLinkPath].");

	}


}
