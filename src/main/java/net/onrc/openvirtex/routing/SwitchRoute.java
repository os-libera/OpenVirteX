/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.routing;

import java.util.Arrays;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.packet.Ethernet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Route within a Big Switch abstraction
 * 
 */
public class SwitchRoute extends Link<OVXPort, PhysicalSwitch> {
    Logger       log = LogManager.getLogger(OVXSwitch.class.getName());

    /** unique route identifier */
    int          routeId;

    /** DPID of parent virtual switch */
    long         dpid;

    /** The Tenant ID of the switch - makes it unique in the physical network */
    int          tenantid;
    
    private final byte priority;
	
    /** A reference to the PhysicalPort at the start of the path */
    PhysicalPort inPort;
	
    /** A reference to the PhysicalPort at the start of the path */
    PhysicalPort outPort;
	
    public SwitchRoute(final OVXPort in, final OVXPort out, final long dpid,
	    final int routeid, final int tid, final byte priority) {
	super(in, out);
	this.dpid = dpid;
	this.routeId = routeid;
	this.tenantid = tid;
	this.priority = priority;
    }

    /**
     * Sets the switch-unique identifier of this route.
     * 
     * @param routeid
     */
    public void setRouteId(final int routeid) {
	this.routeId = routeid;
    }

    /**
     * @return the ID of this route
     */
    public int getRouteId() {
	return this.routeId;
    }

    public byte getPriority() {
        return priority;
    }
    
    /**
     * Sets the DPID of the parent switch of route
     * 
     * @param dpid
     */
    public void setSwitchId(final long dpid) {
	this.dpid = dpid;
    }

    /**
     * @return the DPID of the virtual switch
     */
    public long getSwitchId() {
	return this.dpid;
    }

    /**
     * Sets the tenant ID of this route's parent switch
     * 
     * @param tid
     */
    public void setTenantId(final int tid) {
	this.tenantid = tid;
    }
	
    public Integer getTenantId() {
	return this.tenantid;
    }

    /**
     * Sets the start PhysicalPort to the path defining the route.
     * 
     * @param start
     */
    public void setPathSrcPort(final PhysicalPort start) {
	this.inPort = start;
    }
	
    /**
     * @return the PhysicalPort at start of the route.
     */
    public PhysicalPort getPathSrcPort() {
	/* have to go fetch the map for the links, bit convoluted */
	return this.inPort;
    }
	
    /**
     * Sets the end PhysicalPort to the path defining the route.
     * 
     * @param end
     */
    public void setPathDstPort(final PhysicalPort end) {
	this.outPort = end;
    }
	
    /**
     * @return the PhysicalPort at end of the route.
     */
    public PhysicalPort getPathDstPort() {
	/* have to go fetch the map for the links, bit convoluted */
	return this.outPort;
    }

    @Override
    public String toString() {
	return "routeId: " + this.routeId + " dpid: " + this.dpid + " inPort: "
	        + this.srcPort == null ? "" : this.srcPort.toString()
	        + " outPort: " + this.dstPort == null ? "" : this.dstPort
	        .toString();
    }
	
    @Override
    /**
     * @return the PhysicalSwitch at the start of the route.
     */
    public PhysicalSwitch getSrcSwitch() {
	return this.srcPort.getPhysicalPort().getParentSwitch();
    }

    @Override
    /**
     * @return the PhysicalSwitch at the end of the route. 
     */
    public PhysicalSwitch getDstSwitch() {
	return this.dstPort.getPhysicalPort().getParentSwitch();
    }
	
    public void generateRouteFMs(final OFFlowMod fm,
	    final LinkedList<OFAction> additionalActions,
	    final OVXPort ovxInPort, final OVXPort ovxOutPort) {
	/*
	 * If the packet has L3 fields (e.g. NOT ARP), change the packet match:
	 * 1) change the fields where the physical ips are stored
	 */
	if (fm.getMatch().getDataLayerType() == Ethernet.TYPE_IPv4)
	    IPMapper.rewriteMatch(ovxInPort.getTenantId(), fm.getMatch());
	
	/*
	 * Get the list of physical links mapped to this virtual link,
	 * in REVERSE ORDER
	 */
	PhysicalPort inPort = null;
	PhysicalPort outPort = null;
	fm.setBufferId(OVXPacketOut.BUFFER_ID_NONE);

	final SwitchRoute reverseRoute = ((OVXBigSwitch) ovxInPort
	        .getParentSwitch()).getRoute(ovxOutPort, ovxInPort);
	try {
	    for (final PhysicalLink phyLink : OVXMap.getInstance().getRoute(
	            reverseRoute)) {
	        if (outPort != null) {
	    	inPort = phyLink.getSrcPort();
	    	fm.getMatch().setInputPort(inPort.getPortNumber());
	    	fm.setLengthU(OVXFlowMod.MINIMUM_LENGTH
	    	        + OVXActionOutput.MINIMUM_LENGTH);
	    	fm.setActions(Arrays.asList((OFAction) new OFActionOutput(
	    	        outPort.getPortNumber(), (short) 0xffff)));
	    	phyLink.getSrcPort().getParentSwitch()
	    	        .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
	    	this.log.debug(
	    	        "Sending big-switch route intermediate fm to sw {}: {}",
	    	        phyLink.getSrcPort().getParentSwitch().getName(), fm);

	        } else {
	    	/*
	             * Last fm. Differs from the others because it can apply
	             * additional actions to the flow
	             */
	    	fm.getMatch()
	    	        .setInputPort(phyLink.getSrcPort().getPortNumber());
	    	int actLenght = 0;
	    	additionalActions.add(new OFActionOutput(ovxOutPort
	    	        .getPhysicalPortNumber(), (short) 0xffff));
	    	fm.setActions(additionalActions);
	    	for (final OFAction act : additionalActions) {
	    	    actLenght += act.getLengthU();
	    	}
	    	fm.setLengthU(OVXFlowMod.MINIMUM_LENGTH + actLenght);
	    	phyLink.getSrcPort().getParentSwitch()
	    	        .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
	    	this.log.debug("Sending big-switch route last fm to sw {}: {}",
	    	        phyLink.getSrcPort().getParentSwitch().getName(), fm);
	        }
	        outPort = phyLink.getDstPort();
	    }
        } catch (LinkMappingException e) {
	    log.warn("Could not fetch route : {}", e);
        }
		
	// TODO: With POX we need to put a timeout between this flows and the
	// first flowMod. Check how to solve
	try {
	    Thread.sleep(5);
	} catch (final InterruptedException e) {
	}
    }
    
    @Override
    public void unregister() {
	this.srcPort.getParentSwitch().getMap().removeRoute(this);
    }
}
