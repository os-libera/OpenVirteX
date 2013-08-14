/**
 * 
 */
package net.onrc.openvirtex.elements.address;


public abstract class IPAddress {
	protected int ip;

	public int getIp() {
		return this.ip;
	}

	public void setIp(final int ip) {
		this.ip = ip;
	}
	
	@Override
	public String toString() {
	    return this.getClass().getName() + "[" + (ip >> 24) + "." + ((ip >> 16) & 0xFF) +
		    "." + ((ip >> 8) & 0xFF) +  "." + (ip & 0xFF) + "]";  
	}

}
