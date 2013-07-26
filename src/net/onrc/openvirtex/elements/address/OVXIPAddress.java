/**
 * 
 */
package net.onrc.openvirtex.elements.address;

/**
 * @author gerola
 * 
 */
public class OVXIPAddress extends IPAddress {
    private int tenantId;

    public int getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final int tenantId) {
	this.tenantId = tenantId;
    }

}
