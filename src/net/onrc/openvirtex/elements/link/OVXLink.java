/**
 * 
 */
package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public class OVXLink extends Link<OVXPort> {
    private int linkId;
    private int tenantId;

    /**
     * 
     */
    public OVXLink() {
	super();
	this.linkId = 0;
	this.tenantId = 0;
    }

    /**
     * @param srcPort
     * @param dstPort
     * @param tenantId
     * @param linkId
     */
    public OVXLink(final OVXPort srcPort, final OVXPort dstPort,
	    final int tenantId, final int linkId) {
	super(srcPort, dstPort);
	this.linkId = linkId;
	this.tenantId = tenantId;
    }

    public int getLinkId() {
	return this.linkId;
    }

    public void setLinkId(final int linkId) {
	this.linkId = linkId;
    }

    public int getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final int tenantId) {
	this.tenantId = tenantId;
    }

}
