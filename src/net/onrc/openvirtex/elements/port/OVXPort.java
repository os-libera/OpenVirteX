/**
 * 
 */
package net.onrc.openvirtex.elements.port;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class OVXPort extends Port {
    private int          tenantId;
    private PhysicalPort physicalPort;
    private OVXSwitch    parentSwitch;

    /**
     * 
     */
    public OVXPort() {
	super();
	this.tenantId = 0;
	this.physicalPort = null;
	this.parentSwitch = null;
    }

    /**
     * @param portNumber
     * @param hardwareAddress
     * @param config
     * @param mask
     * @param advertise
     * @param isEdge
     * @param tenantId
     * @param physicalPort
     * @param parentSwitch
     */
    public OVXPort(final short portNumber, final MACAddress hardwareAddress,
	    final int config, final int mask, final int advertise,
	    final Boolean isEdge, final int tenantId,
	    final PhysicalPort physicalPort, final OVXSwitch parentSwitch) {
	super(portNumber, hardwareAddress, config, mask, advertise, isEdge);
	this.tenantId = tenantId;
	this.physicalPort = physicalPort;
	this.parentSwitch = parentSwitch;
    }

    public int getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final int tenantId) {
	this.tenantId = tenantId;
    }

    public PhysicalPort getPhysicalPort() {
	return this.physicalPort;
    }

    public boolean setPhysicalPort(final PhysicalPort physicalPort) {
	if (this.physicalPort != null) {
	    return false;
	} else {
	    this.physicalPort = physicalPort;
	    return true;
	}
    }

    boolean updatePhysicalPort(final PhysicalPort physicalPort) {
	if (this.physicalPort == null) {
	    return false;
	} else {
	    this.physicalPort = physicalPort;
	    return true;
	}
    }

    public OVXSwitch getParentSwitch() {
	return this.parentSwitch;
    }

    public void setParentSwitch(final OVXSwitch parentSwitch) {
	this.parentSwitch = parentSwitch;
    }

    public OVXPort getCopy() {
	return new OVXPort(this.portNumber, this.hardwareAddress, this.config,
	        this.mask, this.advertise, this.isEdge, this.tenantId,
	        this.physicalPort, this.parentSwitch);
    }

}
