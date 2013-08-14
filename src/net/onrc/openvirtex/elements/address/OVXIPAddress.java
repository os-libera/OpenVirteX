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
	
	public OVXIPAddress(int tenantId, int ip) {
	    this.tenantId = tenantId;
	    this.ip = ip;
	}

	public int getTenantId() {
		return this.tenantId;
	}

	public void setTenantId(final int tenantId) {
		this.tenantId = tenantId;
	}

}
