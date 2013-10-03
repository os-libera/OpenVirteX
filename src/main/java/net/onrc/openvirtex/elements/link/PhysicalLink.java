/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * The Class PhysicalLink.
 * 
 */
public class PhysicalLink extends Link<PhysicalPort, PhysicalSwitch> {

	/**
	 * Instantiates a new physical link.
	 * 
	 * @param srcPort
	 *            the source port
	 * @param dstPort
	 *            the destination port
	 */
	public PhysicalLink(final PhysicalPort srcPort, final PhysicalPort dstPort) {
		super(srcPort, dstPort);
		srcPort.setOutLink(this);
		dstPort.setInLink(this);
	}
}
