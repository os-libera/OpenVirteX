/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.LinkPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus;

public class OVXPortStatus extends OFPortStatus implements Virtualizable {
    	
	@Override
	public void virtualize(final PhysicalSwitch sw) {
	    	
	    	PhysicalPort p = sw.getPort(this.desc.getPortNumber());
	    	if (p == null) {
	    	    	/* add a new port to PhySwitch if add message, quit otherwise */
	    	    	if (this.reason == OFPortReason.OFPPR_ADD.getReasonCode()) {
	    	    	    	p = new PhysicalPort(this.desc, sw, false);
	    	    	    	if (!sw.addPort(p)) {
	    	    	    	    	/* handle error - couldn't add port */
	    	    	    	}
	    	    	} 
	    	    	return;
	    	}
	    	
	    	if (this.reason == OFPortReason.OFPPR_DELETE.getReasonCode()) {
			LinkPair<PhysicalLink> lpair = p.getLink();
			/* phy port associated with a phy link */
	    	    	if ((lpair != null) && (lpair.exists())) {
				PhysicalLink plink = lpair.getOutLink();
				Mappable map = sw.getMap();
				for (Map.Entry<Integer, OVXNetwork> tenant : map.
						listVirtualNetworks().entrySet()) {
				    	List<OVXLink> vlinks = map.getVirtualLinks(plink, tenant.getKey());
				    	Set<SwitchRoute> routes = map.getSwitchRoutes(plink, tenant.getKey());
				    	/* within one vNet - either route or vLink, or neither */
				    	if (vlinks != null) {
						for (OVXLink vlink : vlinks) {
							//TODO : try to look for backups, then 
							// tear down vlink if not possible
						}
				    	} else if (routes != null) {
						for (SwitchRoute route : routes) {
							//TODO : try to look for backups    
							// remove route if not possible 
						}
				    	}
				    	/* shut down other phyport, then phylink */
				}
				
			}
	    	    	Set<OVXPort> vports = p.getOVXPorts();
	    	    	/* phy port is mapped to vport */
	    	    	if (vports != null) {
				for (OVXPort vport : vports) {
					/* Route edge, vLink edge, or both */
					LinkPair<OVXLink> vpair = vport.getLink();
					if ((vpair != null) && (vpair.exists())) {
						//TODO delete link and escalate error
					}
					OVXSwitch vsw = vport.getParentSwitch();
					if (vsw instanceof OVXBigSwitch) {
						/* see if this port is edge of any routes */
					    	OVXBigSwitch bvs = (OVXBigSwitch)vsw;
						Set<SwitchRoute> routes = bvs.getRoutebyPort(vport);     	
						for (SwitchRoute route : routes) {
							//TODO remove route, its backup paths, send FlowRemoved
							//need flows associated with port	  
						}
					}
					vport.virtualizePortStat(this);
					vsw.sendMsg(this, sw);
				}
				//remove vports
			}
	    	    	// remove phyport
		} else if (this.reason == OFPortReason.OFPPR_MODIFY.getReasonCode()) {
	    	    	if (this.desc.getState() == OFPortState.OFPPS_LINK_DOWN.getValue()) {
	    	    	    	// TODO: try to recover if 1) port is internal to a BVS
	    	    	    	// and 2) it is part of a route. 
	    	    	    	return;
	    	    	}
	    	    	p.applyPortStatus(this);
	    	    	for (OVXPort vport : p.getOVXPorts()) {
				OVXSwitch vsw = vport.getParentSwitch();
				vport.applyPortStatus(this);
				vport.virtualizePortStat(this);
				vsw.sendMsg(this, sw);
			}
		}	    	
	}
	
}
