/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public abstract class OVXSwitch extends Switch<OVXPort> {
    private int   tenantId;
    private short pktLenght;

    /**
     * 
     */
    public OVXSwitch() {
	super();
	this.tenantId = 0;
	this.pktLenght = 0;
    }

    /**
     * @param switchName
     * @param switchId
     * @param map
     */
    public OVXSwitch(final String switchName, final long switchId,
	    final OVXMap map, final int tenantId, final short pktLenght) {
	super(switchName, switchId, map);
	this.tenantId = tenantId;
	this.pktLenght = pktLenght;
    }

    public int getTenantId() {
	return this.tenantId;
    }

    public boolean setTenantId(final int tenantId) {
	this.tenantId = tenantId;
	return true;
    }

    public short getPktLenght() {
	return this.pktLenght;
    }

    public boolean setPktLenght(final short pktLenght) {
	this.pktLenght = pktLenght;
	return true;
    }

}
