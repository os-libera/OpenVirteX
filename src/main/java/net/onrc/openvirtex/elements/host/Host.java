/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.host;

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

public class Host {
	private final MACAddress mac;
	private final OVXPort port;

	public Host(final MACAddress mac, final OVXPort port) {
		this.mac = mac;
		this.port = port;
	}

	public MACAddress getMac() {
		return mac;
	}

	public OVXPort getPort() {
		return port;
	}
}
