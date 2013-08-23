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

    public IPAddress() {
    }

    public int getIp() {
	return this.ip;
    }

    public void setIp(final int ip) {
	this.ip = ip;
    }

    @Override
    public String toString() {
	return this.getClass().getName() + "[" + (this.ip >> 24) + "."
	        + (this.ip >> 16 & 0xFF) + "." + (this.ip >> 8 & 0xFF) + "."
	        + (this.ip & 0xFF) + "]";
    }

}
