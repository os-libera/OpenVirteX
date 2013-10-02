/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.address;

public class OVXIPAddress extends IPAddress {

	private final int tenantId;

	public OVXIPAddress(final int tenantId, final int ip) {
		super();
		this.tenantId = tenantId;
		this.ip = ip;
	}

	public OVXIPAddress(final String ipAddress, final int tenantId) {
		super(ipAddress);
		this.tenantId = tenantId;
	}

	public int getTenantId() {
		return this.tenantId;
	}

}
