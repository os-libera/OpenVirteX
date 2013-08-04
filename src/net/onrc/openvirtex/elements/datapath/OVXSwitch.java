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

package net.onrc.openvirtex.elements.datapath;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedList;
import java.util.Set;


import net.onrc.openvirtex.elements.port.OVXPort;

import net.onrc.openvirtex.exceptions.IllegalVirtualSwitchConfiguration;
import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;

/**
 * The Class OVXSwitch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> {

    	/** 
    	 *  Datapath description string 
    	 *  should this be made specific per type of virtual switch
    	 */
    	public static final String DPDESCSTRING = "OpenVirteX Virtual Switch"; 
    
	/** The supported actions. */
	protected static int supportedActions = 0xFFF;
	
	/** The buffer dimension. */
	protected static int bufferDimension = 4096;

	/** The tenant id. */
	protected Integer tenantId = 0;
	
	/** The miss send len. */
	protected Short missSendLen = 0;
	
	/** The is active. */
	protected boolean isActive = false;
	
	/** The capabilities. */
	protected OVXSwitchCapabilities capabilities;
	
	/** The backoff counter for this switch when unconnected */
	private AtomicInteger backOffCounter = null;

	// TODO Add the packetIn buffer Map

	/**
	 * Instantiates a new oVX switch.
	 */
	protected OVXSwitch() {
		super();
		this.capabilities = new OVXSwitchCapabilities();
		this.backOffCounter = new AtomicInteger();
		this.resetBackOff();
	}

	/**
	 * Instantiates a new oVX switch.
	 *
	 * @param switchId the switch id
	 * @param tenantId the tenant id
	 */
	protected OVXSwitch(final Long switchId, final Integer tenantId) {
		this();
		this.switchId = switchId;
		this.tenantId = tenantId;
		this.missSendLen = 0;
		this.isActive = false;
		this.switchName = "OpenVirteX Virtual Switch 1.0";
		
	}

	/**
	 * Gets the tenant id.
	 *
	 * @return the tenant id
	 */
	public Integer getTenantId() {
		return this.tenantId;
	}

	/**
	 * Sets the tenant id.
	 *
	 * @param tenantId the tenant id
	 * @return true, if successful
	 */
	public boolean setTenantId(final Integer tenantId) {
		this.tenantId = tenantId;
		return true;
	}

	/**
	 * Gets the miss send len.
	 *
	 * @return the miss send len
	 */
	public short getMissSendLen() {
		return this.missSendLen;
	}

	/**
	 * Sets the miss send len.
	 *
	 * @param missSendLen the miss send len
	 * @return true, if successful
	 */
	public boolean setMissSendLen(final Short missSendLen) {
		this.missSendLen = missSendLen;
		return true;
	}


	/**
	 * Checks if is active.
	 *
	 * @return true, if is active
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Sets the active.
	 *
	 * @param isActive the new active
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * Gets the physical port number.
	 *
	 * @param ovxPortNumber the ovx port number
	 * @return the physical port number
	 */
	public Short getPhysicalPortNumber(Short ovxPortNumber) {
		return this.portMap.get(ovxPortNumber).getPhysicalPortNumber();
	}
	
	
	    public void resetBackOff() {
		this.backOffCounter.set(-1);
	    }

	    public int incrementBackOff() {
		return this.backOffCounter.incrementAndGet();
	   }

	/**
	 * Gets the new port number.
	 *
	 * @return the new port number
	 */
	private Short getNewPortNumber() {
		short portNumber = 1;
		Set<Short> keys = this.portMap.keySet();

		if (keys.isEmpty())
			return portNumber;
		else {
			boolean solved = false;
			while (solved == false && portNumber < 256) {
				if (!keys.contains(portNumber))
					solved = true;
				else
					portNumber += 1;
			}
			if (solved == true)
				return portNumber;
			else
				return 0;
		}
	}

	/**
	 * Creates the port.
	 *
	 * @param isEdge the is edge
	 * @param hwAddress the hw address
	 * @return the short
	 */
	public Short createPort(boolean isEdge, MACAddress hwAddress) {
		Short ovxPortNumber = getNewPortNumber();
		if (ovxPortNumber != 0) {
			OVXPort ovxPort = new OVXPort(ovxPortNumber, hwAddress, isEdge,
					this.tenantId, this);
			this.portMap.put(ovxPortNumber, ovxPort);
		}

		return ovxPortNumber;
	}

	/**
	 * Register port.
	 *
	 * @param ovxPortNumber the ovx port number
	 * @param physicalSwitchId the physical switch id
	 * @param physicalPortNumber the physical port number
	 * @return true, if successful
	 * @throws IllegalVirtualSwitchConfiguration 
	 */
	public abstract boolean registerPort(Short ovxPortNumber, Long physicalSwitchId,
			Short physicalPortNumber) throws IllegalVirtualSwitchConfiguration;
		

	/**
	 * Generate features reply.
	 */
	protected void generateFeaturesReply() {
		OFFeaturesReply ofReply = new OFFeaturesReply();
		ofReply.setDatapathId(this.switchId);
		LinkedList<OFPhysicalPort> portList = new LinkedList<OFPhysicalPort>();
		for (Short portNumber : this.portMap.keySet()) {
			OFPhysicalPort port = new OFPhysicalPort();
			port.setPortNumber(portNumber);
			port.setName(port.getName());
			port.setConfig(port.getConfig());
			port.setHardwareAddress(port.getHardwareAddress());
			port.setState(port.getState());
			port.setAdvertisedFeatures(port.getAdvertisedFeatures());
			port.setCurrentFeatures(port.getCurrentFeatures());
			port.setSupportedFeatures(port.getSupportedFeatures());
			portList.add(port);
		}
		ofReply.setPorts(portList);
		ofReply.setBuffers(bufferDimension);
		ofReply.setTables((byte) 1);
		ofReply.setCapabilities(this.capabilities.getOVXSwitchCapabilities());
		ofReply.setActions(supportedActions);
		ofReply.setXid(0);
		ofReply.setLength((short) (OFFeaturesReply.MINIMUM_LENGTH + (OFPhysicalPort.MINIMUM_LENGTH * portList
				.size())));

		setFeaturesReply(ofReply);
	}

	

	/* (non-Javadoc)
	 * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
	 */
	@Override
	public String toString() {
		return "SWITCH: switchId: " + this.switchId + " - switchName: "
				+ this.switchName + " - isConnected: " + this.isConnected
				+ " - tenantId: " + this.tenantId + " - missSendLength: "
				+ this.missSendLen + " - isActive: " + this.isActive
				+ " - capabilities: "
				+ this.capabilities.getOVXSwitchCapabilities();
	}
}
