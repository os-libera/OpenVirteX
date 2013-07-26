/**
 * 
 */
package net.onrc.openvirtex.elements.address;

/**
 * @author gerola
 * 
 */
public abstract class IPAddress {
    private int ip;

    public int getIp() {
	return this.ip;
    }

    public void setIp(final int ip) {
	this.ip = ip;
    }

}
