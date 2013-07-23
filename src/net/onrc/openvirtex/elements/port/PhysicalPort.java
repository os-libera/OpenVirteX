/**
 * 
 */
package net.onrc.openvirtex.elements.port;

import java.util.HashMap;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class PhysicalPort extends Port {
    private final HashMap<Integer, OVXPort> ovxPortMap;
    private PhysicalSwitch                  parentSwitch;

    /**
     * 
     */
    public PhysicalPort() {
	super();
	this.ovxPortMap = new HashMap<Integer, OVXPort>();
	this.parentSwitch = null;
    }

    /**
     * @param portNumber
     * @param hardwareAddress
     * @param config
     * @param mask
     * @param advertise
     * @param isEdge
     * @param parentSwitch
     */
    public PhysicalPort(final short portNumber,
	    final MACAddress hardwareAddress, final int config, final int mask,
	    final int advertise, final Boolean isEdge,
	    final PhysicalSwitch parentSwitch) {
	super(portNumber, hardwareAddress, config, mask, advertise, isEdge);
	this.ovxPortMap = new HashMap<Integer, OVXPort>();
	this.parentSwitch = parentSwitch;
    }

    public OVXPort getOVXPort(final int tenantId) {
	return this.ovxPortMap.get(tenantId);
    }

    public boolean setOVXPort(final int tenantId, final OVXPort ovxPort) {
	if (this.ovxPortMap.containsKey(tenantId)) {
	    return false;
	} else {
	    this.ovxPortMap.put(tenantId, ovxPort);
	}
	return true;
    }

    public boolean updateOVXPort(final int tenantId, final OVXPort ovxPort) {
	if (!this.ovxPortMap.containsKey(tenantId)) {
	    return false;
	} else {
	    this.ovxPortMap.put(tenantId, ovxPort);
	}
	return true;
    }

    public PhysicalSwitch getParentSwitch() {
	return this.parentSwitch;
    }

    public boolean setParentSwitch(final PhysicalSwitch parentSwitch) {
	this.parentSwitch = parentSwitch;
	return true;
    }

    public PhysicalPort getCopy() {
	return new PhysicalPort(this.portNumber, this.hardwareAddress,
	        this.config, this.mask, this.advertise, this.isEdge,
	        this.parentSwitch);
    }

}
