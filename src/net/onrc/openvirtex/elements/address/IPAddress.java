/**
 * 
 */
package net.onrc.openvirtex.elements.address;

import net.onrc.openvirtex.packet.IPv4;

public class IPAddress {
	private int ip;

	public IPAddress(final String ipAddress) {
	    this.ip = IPv4.toIPv4Address(ipAddress);
	}
	
	public int getIp() {
		return this.ip;
	}

	public void setIp(final int ip) {
		this.ip = ip;
	}
	
	public String toString() {
	    return IPv4.fromIPv4Address(this.ip);
	}

}
