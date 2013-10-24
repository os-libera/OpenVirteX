/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.port;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.messages.OVXPortStatus;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

public class OVXPort extends Port<OVXSwitch, OVXLink> {
    
    private static Logger log = LogManager.getLogger(OVXPort.class
		.getName());
    
    private final Integer      tenantId;
    private final PhysicalPort physicalPort;
    private boolean isActive; 

    public OVXPort(final int tenantId, final PhysicalPort port,
	    final boolean isEdge) throws IndexOutOfBoundException {
	super(port);
	this.tenantId = tenantId;
	this.physicalPort = port;
	try {		    
	    this.parentSwitch = OVXMap.getInstance().getVirtualSwitch(
		    port.getParentSwitch(), tenantId);
	} catch (SwitchMappingException e) {
	    // something pretty wrong if we get here. Not 100% on how to handle this
	    throw new RuntimeException("Unexpected state in OVXMap: " + e.getMessage());
	}
	this.portNumber = this.parentSwitch.getNextPortNumber();
	this.name = "ovxport-"+this.portNumber;
	this.isEdge = isEdge;
	this.hardwareAddress = port.getHardwareAddress();
	PortFeatures features = new PortFeatures();
	features.setCurrentOVXPortFeatures();
	this.currentFeatures = features.getOVXFeatures();
	features.setAdvertisedOVXPortFeatures();
	this.advertisedFeatures = features.getOVXFeatures();
	features.setSupportedOVXPortFeatures();
	this.supportedFeatures = features.getOVXFeatures();
	features.setPeerOVXPortFeatures();
	this.peerFeatures = features.getOVXFeatures();
	this.state = OFPortState.OFPPS_LINK_DOWN.getValue();
	this.config = 0;
	this.isActive = false;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
	
    public boolean isLink() {
	if (this.portLink == null)
	    return false;
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

    public void boot() {
	this.isActive = true;
	this.state = OFPortState.OFPPS_STP_FORWARD.getValue();
	this.parentSwitch.generateFeaturesReply();
	if (this.parentSwitch.isActive())
	    sendStatusMsg(OFPortReason.OFPPR_MODIFY);
    }
    
    public void tearDown() {
	this.isActive = false;
	this.state = OFPortState.OFPPS_LINK_DOWN.getValue();
	this.parentSwitch.generateFeaturesReply();
	if (this.parentSwitch.isActive())
	    sendStatusMsg(OFPortReason.OFPPR_MODIFY);
    }
    
    public void unregister() {
	OVXNetwork virtualNetwork = null;
	try {
	    virtualNetwork = this.parentSwitch.getMap().getVirtualNetwork(this.tenantId);
	} catch (NetworkMappingException e) {
	    log.error("Error retrieving the network with id {}. Unregister for OVXPort {}/{} not fully done!", 
		    this.getTenantId(), this.getParentSwitch().getSwitchName(), this.getPortNumber());
	    return;
	}        
	this.parentSwitch.removePort(this.portNumber);
	this.physicalPort.removeOVXPort(this);
	if (this.parentSwitch.isActive()) {
	    sendStatusMsg(OFPortReason.OFPPR_DELETE);
	}
	if (this.isEdge && this.isActive) {
	    Host host = virtualNetwork.getHost(this);
	    this.parentSwitch.getMap().removeMAC(host.getMac());
	    virtualNetwork.removeHost(host);
	}
	else {
	    this.getLink().egressLink.unregister();
	    this.getLink().ingressLink.unregister();
	}
	this.parentSwitch.generateFeaturesReply();
    }

    public boolean equals(final OVXPort port) {
	return this.portNumber == port.portNumber
		&& this.parentSwitch.getSwitchId() == port.getParentSwitch()
		.getSwitchId();
    }
}
