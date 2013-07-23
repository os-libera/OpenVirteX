/**
 * 
 */
package net.onrc.openvirtex.elements.port;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public abstract class Port implements Mappable {
    protected short      portNumber;
    protected MACAddress hardwareAddress;
    protected int        config;
    protected int        mask;
    protected int        advertise;

    // dovremmo mettere duplixing/speed anche
    // vedere come averli

    protected Boolean    isEdge;

    /**
     * 
     */
    public Port() {
	super();
	this.portNumber = 0;
	this.hardwareAddress = null;
	this.config = 0;
	this.mask = 0;
	this.advertise = 0;
	this.isEdge = true;
    }

    /**
     * @param portNumber
     * @param hardwareAddress
     * @param config
     * @param mask
     * @param advertise
     * @param isEdge
     */
    public Port(final short portNumber, final MACAddress hardwareAddress,
	    final int config, final int mask, final int advertise,
	    final Boolean isEdge) {
	super();
	this.portNumber = portNumber;
	this.hardwareAddress = hardwareAddress;
	this.config = config;
	this.mask = mask;
	this.advertise = advertise;
	this.isEdge = isEdge;
    }

    public short getPortNumber() {
	return this.portNumber;
    }

    public void setPortNumber(final short portNumber) {
	this.portNumber = portNumber;
    }

    public MACAddress getHardwareAddress() {
	return this.hardwareAddress;
    }

    public void setHardwareAddress(final MACAddress hardwareAddress) {
	this.hardwareAddress = hardwareAddress;
    }

    public int getConfig() {
	return this.config;
    }

    public void setConfig(final int config) {
	this.config = config;
    }

    public int getMask() {
	return this.mask;
    }

    public void setMask(final int mask) {
	this.mask = mask;
    }

    public int getAdvertise() {
	return this.advertise;
    }

    public void setAdvertise(final int advertise) {
	this.advertise = advertise;
    }

    public Boolean getIsEdge() {
	return this.isEdge;
    }

    public void setIsEdge(final Boolean isEdge) {
	this.isEdge = isEdge;
    }

}
