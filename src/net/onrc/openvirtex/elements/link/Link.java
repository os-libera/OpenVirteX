/**
 * 
 */
package net.onrc.openvirtex.elements.link;

/**
 * @author gerola
 * 
 */
public abstract class Link<T1> {
    private T1 srcPort;
    private T1 dstPort;

    /**
     * 
     */
    public Link() {
	super();
	this.srcPort = null;
	this.dstPort = null;
    }

    /**
     * @param srcPort
     * @param dstPort
     */
    public Link(final T1 srcPort, final T1 dstPort) {
	super();
	this.srcPort = srcPort;
	this.dstPort = dstPort;
    }

    public T1 getSrcPort() {
	return this.srcPort;
    }

    public void setSrcPort(final T1 srcPort) {
	this.srcPort = srcPort;
    }

    public T1 getDstPort() {
	return this.dstPort;
    }

    public void setDstPort(final T1 dstPort) {
	this.dstPort = dstPort;
    }

}
