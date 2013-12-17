/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.LinkPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
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
			handlePortAdd(sw, p); 
	    	    	return;
	    	}
		
		log.info("Received {} from switch {}", this.toString(), sw.getSwitchId());		
		LinkPair<PhysicalLink> pair = p.getLink();
		try {
			Set<Integer> vnets = map.listVirtualNetworks().keySet();
			for ( Integer tenantId : vnets) {
				/* handle vLinks/routes containing phyLink to/from this port. */    
				if ((pair != null) && (pair.exists())) {
					handleLinkChange(sw, map, pair, tenantId);
				}
				List<Map<Integer, OVXPort>> vports = p.getOVXPorts(tenantId);
				/* cycle through all OVXPorts for this port. */
				Iterator<Map<Integer, OVXPort>> p_itr = vports.iterator();
				while(p_itr.hasNext()) {
					Map<Integer, OVXPort> mp = p_itr.next();
					if (mp == null) {
						continue;
					}
					for (Map.Entry<Integer, OVXPort> p_map : mp.entrySet()) {
						OVXPort vport = p_map.getValue();
						if (vport == null) {
							continue;
						}
						if (isReason(OFPortReason.OFPPR_DELETE)) {
							/* try to remove OVXPort, vLinks, routes */
							vport.unMapHost();
							vport.handlePortDelete(this);
							sw.removePort(p);
						} else if (isReason(OFPortReason.OFPPR_MODIFY)) {
							if (isState(OFPortState.OFPPS_LINK_DOWN)) {
								/* set ports as edge, but don't remove vLinks */
								vport.handlePortDisable(this);
							} else if (!isState(OFPortState.OFPPS_LINK_DOWN) && 
									((p.getState() & OFPortState.OFPPS_LINK_DOWN.getValue()) == 0)) {
								/* set links to non-edge, if it was previously disabled */
								vport.handlePortEnable(this);
							} 
						}
					}
				}
		    }
		} catch (NetworkMappingException | LinkMappingException e) {
                    log.warn("Couldn't process reason={} for PortStatus for port {}", 
                	    this.reason, p.getPortNumber());
                    e.printStackTrace();
                }	
	} 
	
	private void handlePortAdd(PhysicalSwitch sw, PhysicalPort p) {
	    /* add a new port to PhySwitch if add message, quit otherwise */
	    if (isReason(OFPortReason.OFPPR_ADD)) {
			p = new PhysicalPort(this.desc, sw, false);
			if (!sw.addPort(p)) {
			    log.warn("Could not add new port {} to physical switch {}", 
				    p.getPortNumber(), sw.getSwitchId());
			}
			log.info("Added port {} to switch {}", p.getPortNumber(), sw.getSwitchId());		
	    }
	}
	
	/**
	 * Handles change in internal link state, e.g a PhysicalPort in, but not at 
	 * edges of, a OVXLink or SwitchRoute
	 * 
	 * @param map Mappable containing global information
	 * @param pair the LinkPair associated with the PhysicalPort
	 * @param tid the tenant ID
	 * @throws LinkMappingException
	 * @throws NetworkMappingException
	 */
	private void handleLinkChange(PhysicalSwitch sw, Mappable map, 
			LinkPair<PhysicalLink> pair, int tid) 
					throws LinkMappingException, NetworkMappingException {
		PhysicalLink plink = pair.getOutLink();
		if (map.hasOVXLinks(plink, tid)) {
			List<OVXLink> vlinks = map.getVirtualLinks(plink, tid);
			for (OVXLink vlink : vlinks) {
				if (isReason(OFPortReason.OFPPR_DELETE)) {
					/* couldn't recover, remove link */
					if (!vlink.tryRecovery(plink)) {
						OVXPort vport = vlink.getSrcPort();
						vport.unMapHost();
						vport.handlePortDelete(this);
						sw.removePort(plink.getSrcPort());
					}
				}
				if (isReason(OFPortReason.OFPPR_MODIFY)) {
					if (isState(OFPortState.OFPPS_LINK_DOWN)) {
						/* couldn't recover, remove link */
						if (!vlink.tryRecovery(plink)) {
							vlink.getSrcPort().handlePortDisable(this);
						}    
					} else if (!isState(OFPortState.OFPPS_LINK_DOWN) && 
							((plink.getSrcPort().getState() & 
									OFPortState.OFPPS_LINK_DOWN.getValue()) == 0)) {
						log.debug("enabling OVXLink mapped to port {}");
						/* try to switch back to original path, 
						 * if not just bring up and hope it's working */
						if (!vlink.tryRevert(plink)) {
							vlink.getSrcPort().handlePortEnable(this);
						}
					} 
				}
			}
		}
		if (map.hasSwitchRoutes(plink, tid)) {
			Set<SwitchRoute> routes = new HashSet<SwitchRoute>(map.getSwitchRoutes(plink, tid));
			for (SwitchRoute route : routes) {
				/* try to recover, remove route if we fail, but don't send any stat up */
				if ((isReason(OFPortReason.OFPPR_DELETE)) ||
						(isReason(OFPortReason.OFPPR_MODIFY) & isState(OFPortState.OFPPS_LINK_DOWN))) {
					if (!route.tryRecovery(plink)) {
						route.getSrcPort().handleRouteDisable(this);
					}
				} else if (!isState(OFPortState.OFPPS_LINK_DOWN) && 
						((plink.getSrcPort().getState() & 
								OFPortState.OFPPS_LINK_DOWN.getValue()) == 0)) {
					route.tryRevert(plink);
				}
			}
		}
	}
	
	public boolean isReason(OFPortReason reason) {
	    return this.reason == reason.getReasonCode();
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
