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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.packet.Ethernet;

/**
 * Route within a Big Switch abstraction
 * 
 */
public class SwitchRoute extends Link<OVXPort, PhysicalSwitch> {
	Logger	log = LogManager.getLogger(OVXSwitch.class.getName());

	/** unique route identifier */
	int routeId;

	/** DPID of parent virtual switch */
	long dpid;

	/** The Tenant ID of the switch - makes it unique in the physical network */
	int tenantid;
	
	/** A reference to the PhysicalPort at the start of the path */
	PhysicalPort inPort;
	
	/** A reference to the PhysicalPort at the start of the path */
	PhysicalPort outPort;
	
	public SwitchRoute(OVXPort in, OVXPort out, final long dpid, final int routeid, final int tid){
	    	super(in, out);
	    	this.dpid = dpid;
		this.routeId = routeid;
		this.tenantid = tid;
	}

	/**
	 * Sets the switch-unique identifier of this route. 
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

	/**
	 * Sets the DPID of the parent switch of route
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
	 * @param tid
	 */
	public void setTenantId(int tid) {
	    	this.tenantid = tid;
	}
	
	public Integer getTenantId() {
	    	return this.tenantid;
	}

	/**
	 * Sets the start PhysicalPort to the path defining the route.
	 * @param start
	 */
	public void setPathSrcPort(PhysicalPort start) {
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
	 * @param end
	 */
	public void setPathDstPort(PhysicalPort end) {
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
		return "routeId: " + this.routeId + " dpid: " + this.dpid
				+ " inPort: "+ this.srcPort == null ? "" : this.srcPort.toString() 
				+ " outPort: "+ this.dstPort == null ? "" : this.dstPort.toString();
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
	
    public void generateRouteFMs(OFFlowMod fm, LinkedList<OFAction> additionalActions, 
    		PhysicalPort endPort, OVXSwitch sw) { 
    	Short inPort = 0;
    	Short outPort = 0;
    	
    	OFFlowMod routeFM = null;
		try {
			routeFM = fm.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			return;
		}
		if (fm.getMatch().getDataLayerType() != Ethernet.TYPE_ARP) 
			rewriteMatch(routeFM, sw);
		routeFM.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		
		//TODO: check if works also with the original match
		OFMatch match = routeFM.getMatch().clone();
		LinkedList<OFFlowMod> fmList = new LinkedList<>();
		LinkedList<PhysicalSwitch> sws = new  LinkedList<PhysicalSwitch>();
		
    	for (PhysicalLink phyLink : sw.getMap().getRoute(this)) {
    		if (inPort != 0) {
    			outPort = phyLink.getSrcPort().getPortNumber();
    			match.setInputPort(inPort);
    			routeFM.setMatch(match);
    			routeFM.setOutPort(outPort);
    			routeFM.setLengthU(OVXFlowMod.MINIMUM_LENGTH + OVXActionOutput.MINIMUM_LENGTH);
    			routeFM.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort, (short) 0xffff)));
    			try {
					fmList.add(0, routeFM.clone());
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			sws.add(0, phyLink.getSrcPort().getParentSwitch());
    			log.debug("Sending flowMod [route-intermediate] to {}: {}", phyLink.getSrcPort().getParentSwitch().getName(), routeFM);
    		}
    		inPort = phyLink.getDstPort().getPortNumber();
    	}
    	
    	//Generating the exit flowMod

    	match.setInputPort(inPort);
    	routeFM.setMatch(match);
    	routeFM.setOutPort(endPort.getPortNumber());
    	additionalActions.add(new OFActionOutput(endPort.getPortNumber(), (short) 0xffff));
    	routeFM.setActions(additionalActions);
    	routeFM.setLengthU(OVXFlowMod.MINIMUM_LENGTH);
    	for (OFAction act : additionalActions) {
    		routeFM.setLengthU(routeFM.getLengthU() + act.getLengthU());
    	}
    	endPort.getParentSwitch().sendMsg(routeFM, null);
    	log.debug("Sending flowMod [route-end] to {}: {}", endPort.getParentSwitch().getName(), routeFM);
		
		for (int i = 0 ; i < sws.size() ; i++)
    		sws.get(i).sendMsg(fmList.get(i), sws.get(i));
		
    	try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
    
    private void rewriteMatch(OFFlowMod fm, OVXSwitch sw) {
    	final Mappable map = sw.getMap();

    	// TODO: handle IP ranges
    	if (!fm.getMatch().getWildcardObj().isWildcarded(Flag.NW_SRC)) {
    		final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
    				fm.getMatch().getNetworkSource());
    		PhysicalIPAddress pip = map.getPhysicalIP(vip,
    				sw.getTenantId());
    		if (pip == null) {
    			pip = new PhysicalIPAddress(map.getVirtualNetwork(
    					sw.getTenantId()).nextIP());
    			this.log.debug(
    					"Adding IP mapping {} -> {} for tenant {} at switch {}",
    					vip, pip, sw.getTenantId(), sw.getName());
    			map.addIP(pip, vip);
    		}
    		fm.getMatch().setNetworkSource(pip.getIp());
    	}

    	if (!fm.getMatch().getWildcardObj().isWildcarded(Flag.NW_DST)) {
    		final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
    				fm.getMatch().getNetworkDestination());
    		PhysicalIPAddress pip = map.getPhysicalIP(vip,
    				sw.getTenantId());
    		if (pip == null) {
    			pip = new PhysicalIPAddress(map.getVirtualNetwork(
    					sw.getTenantId()).nextIP());
    			this.log.debug(
    					"Adding IP mapping {} -> {} for tenant {} at switch {}",
    					vip, pip, sw.getTenantId(), sw.getName());
    			map.addIP(pip, vip);
    		}
    		fm.getMatch().setNetworkDestination(pip.getIp());
    	}

    }

	@Override
	public void unregister() {
		this.srcPort.getParentSwitch().getMap().removeRoute(this);
	}
}
