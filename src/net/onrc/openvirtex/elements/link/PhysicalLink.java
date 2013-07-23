/**
 * 
 */
package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * @author gerola
 * 
 */
public class PhysicalLink extends Link<PhysicalPort> {

    /**
     * 
     */
    public PhysicalLink() {
	super();
	// TODO Auto-generated constructor stub
    }

    /**
     * @param srcPort
     * @param dstPort
     */
    public PhysicalLink(final PhysicalPort srcPort, final PhysicalPort dstPort) {
	super(srcPort, dstPort);
    }

}
