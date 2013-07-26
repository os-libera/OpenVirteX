/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.elements.port;

import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class Port implements Cloneable {
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
    protected Port() {}

    /**
     * @param portNumber
     * @param hardwareAddress
     * @param config
     * @param mask
     * @param advertise
     * @param isEdge
     */
    protected Port(final short portNumber, final MACAddress hardwareAddress,
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
    
    @Override
    public Object clone() throws CloneNotSupportedException {
	//Log error throw exception
	throw new CloneNotSupportedException("The base class should never be cloned.");
    }
    

}
