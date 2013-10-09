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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus;

public class OVXPortStatus extends OFPortStatus implements Virtualizable {
    
    	private final Logger log      = LogManager.getLogger(OVXPortStatus.class);
    	
	@Override
	public void virtualize(final PhysicalSwitch sw) {
		Mappable map = sw.getMap();
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
		List<Map<Integer, OVXPort>> vports = p.getOVXPorts(null);
    	    	if (this.reason == OFPortReason.OFPPR_DELETE.getReasonCode()) {
	    		log.info("Received {} from switch {}", this.toString(), sw.getSwitchId());
    	    	
			LinkPair<PhysicalLink> lpair = p.getLink();
			/* phy port associated with a phy link */
	    	    	if ((lpair != null) && (lpair.exists())) {
				PhysicalLink plink = lpair.getOutLink();
				for (Map.Entry<Integer, OVXNetwork> tenant : map.
						listVirtualNetworks().entrySet()) {
				    	List<OVXLink> vlinks = map.getVirtualLinks(plink, tenant.getKey());
				    	Set<SwitchRoute> routes = map.getSwitchRoutes(plink, tenant.getKey());
				    	/* within one vNet - either route or vLink, or neither */
				    	if (vlinks != null) {
						for (OVXLink vlink : vlinks) {
							//TODO : try to look for backups 
							/* tear down vlinks and escalate error if no backups */
								    
							    
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
	    	    	/* phy port is mapped to vport */
	    	    	for (Map<Integer, OVXPort> vmap : vports) {
	    	    	    	/* handle per-tenant */
				for (OVXPort vp : vmap.values() ) {
					/* Route edge, vLink edge, or both */
					LinkPair<OVXLink> vpair = vp.getLink();
					if ((vpair != null) && (vpair.exists())) {
						//TODO delete link and escalate error
					}
					OVXSwitch vsw = vp.getParentSwitch();
					if (vsw instanceof OVXBigSwitch) {
						/* see if this port is edge of any routes */
				    		OVXBigSwitch bvs = (OVXBigSwitch)vsw;
						Set<SwitchRoute> routes = bvs.getRoutebyPort(vp);     	
						for (SwitchRoute route : routes) {
							//TODO remove route, its backup paths, send FlowRemoved
							//need flows associated with port	  
						}
					}
					vp.virtualizePortStat(this);
					log.info("Sending {} as OVXSwitch {}", this.toString(), vsw.getSwitchId());
					vsw.sendMsg(this, sw);
	    	    	    	}
			}
			//remove vports
	    	    	// remove phyport
		} else if (this.reason == OFPortReason.OFPPR_MODIFY.getReasonCode()) {
			log.info("Received {} from switch {}", this.toString(), sw.getSwitchId());
			for (Map.Entry<Integer, OVXNetwork> tenant : map.
					listVirtualNetworks().entrySet()) {
			    	/* only 1 element when ports fetched by tenantID */
				Map<Integer, OVXPort> tports = p.getOVXPorts(tenant.getKey()).get(0);    
				if ((tports != null) && 
					this.isState(OFPortState.OFPPS_LINK_DOWN) && (tports.isEmpty())) {
					// TODO: try to recover, if port is internal to a BVS
					// and is part of a route. 
	    	    	    		return;
				}
				p.applyPortStatus(this);
		    	    	for (Map<Integer, OVXPort> vmap : vports) {
		    	    		for (OVXPort vp : vmap.values() ) {
						OVXSwitch vsw = vp.getParentSwitch();
						vp.applyPortStatus(this);
						vp.virtualizePortStat(this);
						log.info("Sending {} as OVXSwitch {}", this.toString(), vsw.getSwitchId());
						vsw.sendMsg(this, sw);
		    		    	}
				}
			}
		}	    	
	}
	
	public boolean isState(OFPortState state) {
		return this.desc.getState() == state.getValue();    
	}
	
	@Override
	public String toString() {
		return "OVXPortStatus: reason[" 
			+ OFPortReason.fromReasonCode(this.reason).name() + "]"
			+ " port[" + this.desc.getPortNumber() +"]";	    	
	}
	
}
