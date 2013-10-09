/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import org.openflow.protocol.OFPortStatus;

import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.messages.OVXPortStatus;

public class OVXPort extends Port<OVXSwitch, OVXLink> {

	private final Integer      tenantId;
	private final PhysicalPort physicalPort;
	/** The link id. */
	private Integer            linkId;

	public OVXPort(final int tenantId, final PhysicalPort port,
			final boolean isEdge) {
		super(port);
		this.tenantId = tenantId;
		this.physicalPort = port;
		this.parentSwitch = OVXMap.getInstance().getVirtualSwitch(
				port.getParentSwitch(), tenantId);
		this.portNumber = this.parentSwitch.getNextPortNumber();
		this.hardwareAddress = port.getHardwareAddress();
		this.isEdge = isEdge;
		// advertise current speed/duplex as 100MB FD, media type copper
		this.currentFeatures = 324;
		// advertise current feature + 1GB FD
		this.advertisedFeatures = 340;
		// advertise all the OF1.0 physical port speeds and duplex. Advertise
		// media type as copper
		this.supportedFeatures = 383;
		this.linkId = 0;
	}

	public Integer getTenantId() {
		return this.tenantId;
	}

	public PhysicalPort getPhysicalPort() {
		return this.physicalPort;
	}

	public Short getPhysicalPortNumber() {
		return this.physicalPort.getPortNumber();
	}

	public Integer getLinkId() {
		return this.linkId;
	}

	public void setLinkId(final Integer linkId) {
		this.linkId = linkId;
	}

	public boolean isLink() {
		if (this.linkId == 0) {
			return false;
		}
		return true;
	}

	public void sendStatusMsg(OFPortReason reason) {
		OFPortStatus status = new OFPortStatus();
		status.setDesc(this);
		status.setReason(reason.getReasonCode());
		this.parentSwitch.sendMsg(status, this.parentSwitch);
	}

	/**
	 * Registers a port in the virtual parent switch and in the physical port
	 */
	public void register() {
		this.parentSwitch.addPort(this);
		this.physicalPort.setOVXPort(this);
		if (this.parentSwitch.isActive()) {
			sendStatusMsg(OFPortReason.OFPPR_ADD);
			this.parentSwitch.generateFeaturesReply();

		}
	}

	/**
	 * Modifies the fields of a OVXPortStatus message so that it is consistent  
	 * with the configs of the corresponding OVXPort. 
	 * 
	 * @param portstat
	 * @return
	 */
	public void virtualizePortStat(OVXPortStatus portstat) {
		OFPhysicalPort desc = portstat.getDesc();
		desc.setPortNumber(this.portNumber);
		desc.setHardwareAddress(this.hardwareAddress);
		desc.setCurrentFeatures(this.currentFeatures);
		desc.setAdvertisedFeatures(this.advertisedFeatures);
		desc.setSupportedFeatures(this.supportedFeatures);
		portstat.setDesc(desc);  
	}

	/**
	 * Changes the attribute of this port according to a MODIFY PortStatus
	 * @param portstat
	 */
	public void applyPortStatus(OVXPortStatus portstat) {
		if (portstat.getReason() != OFPortReason.OFPPR_MODIFY.getReasonCode()) {    	
			return;    
		}
		OFPhysicalPort psport = portstat.getDesc();
		this.config = psport.getConfig();    
		this.state = psport.getState();    
		this.peerFeatures = psport.getPeerFeatures();
	}


	public void unregister() {
		this.parentSwitch.removePort(this.portNumber);
		this.physicalPort.removeOVXPort(this);
		if (this.parentSwitch.isActive()) {
			sendStatusMsg(OFPortReason.OFPPR_DELETE);
			this.parentSwitch.generateFeaturesReply();
		}
		if (this.isEdge) {
			OVXNetwork virtualNetwork = this.parentSwitch.getMap().getVirtualNetwork(this.tenantId);
			Host host = virtualNetwork.getHost(this);
			this.parentSwitch.getMap().removeMAC(host.getMac());
			virtualNetwork.removeHost(host.getMac());
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		result += "\n- tenantId: " + this.tenantId + "\n- linkId: "
				+ this.linkId;
		return result;
	}

	public boolean equals(final OVXPort port) {
		return this.portNumber == port.portNumber
				&& this.parentSwitch.getSwitchId() == port.getParentSwitch()
				.getSwitchId();
	}
}
