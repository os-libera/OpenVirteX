/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
/**
 * 
 */
package net.onrc.openvirtex.elements.address;

import net.onrc.openvirtex.packet.IPv4;

public abstract class IPAddress {
	protected int ip;

	protected IPAddress(final String ipAddress) {
		this.ip = IPv4.toIPv4Address(ipAddress);
	}

	protected IPAddress() {
	}

	public int getIp() {
		return this.ip;
	}

	public void setIp(final int ip) {
		this.ip = ip;
	}

	public String toSimpeString() {
		return (this.ip >> 24) + "."
				+ (this.ip >> 16 & 0xFF) + "." + (this.ip >> 8 & 0xFF) + "."
				+ (this.ip & 0xFF);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + (this.ip >> 24) + "."
				+ (this.ip >> 16 & 0xFF) + "." + (this.ip >> 8 & 0xFF) + "."
				+ (this.ip & 0xFF) + "]";
	}

	@Override
	public boolean equals(final Object that) {
		if (that instanceof IPAddress) {
			return this.ip == ((IPAddress) that).ip;
		}
		return false;
	}

}
