/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.packet.Ethernet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

/**
 * Route within a Big Switch abstraction
 * 
 */
public class SwitchRoute extends Link<OVXPort, PhysicalSwitch> implements Persistable {
	Logger       log = LogManager.getLogger(SwitchRoute.class.getName());

	public static final String DB_KEY = "routes";

	/** unique route identifier */
	int          routeId;

	/** DPID of parent virtual switch */
	long         dpid;

	/** The Tenant ID of the switch - makes it unique in the physical network */
	int          tenantid;

	private byte priority;

	private final TreeMap<Byte, List<PhysicalLink>> backupRoutes;
	private final TreeMap<Byte, List<PhysicalLink>> unusableRoutes;

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
		this.backupRoutes = new TreeMap<>();
		this.unusableRoutes = new TreeMap<>();
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

	public void setPriority(byte priority) {
		this.priority = priority;
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

	public void addBackupRoute(Byte priority, final List<PhysicalLink> physicalLinks) {
		this.backupRoutes.put(priority, physicalLinks);
	}

	public void replacePrimaryRoute(Byte priority, final List<PhysicalLink> physicalLinks) {
		//Save the current path in the backup Map
		try {
			this.addBackupRoute(this.getPriority(), OVXMap.getInstance().getRoute(this));
		} catch (LinkMappingException e) {
			log.error("Unable to retrieve the list of physical link from the OVXMap associated to the big-switch route {}" , 
					this.getRouteId());
		}

		this.switchPath(physicalLinks, priority);
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

	public void switchPath(List<PhysicalLink> physicalLinks, byte priority) {
		//Register the new path as primary path in the OVXMap
		OVXMap.getInstance().removeRoute(this);
		OVXMap.getInstance().addRoute(this, physicalLinks);
		//Set the route priority to the new one 
		this.setPriority(priority);

		int counter = 0;
		log.info("Virtual network {}: switching all existing flow-mods crossing the big-switch {} route {} between ports ({},{}) to the new path: {}", 
				this.getTenantId(), this.getSrcPort().getParentSwitch().getSwitchName(), this.getRouteId(), this.getSrcPort().getPortNumber(),
				this.getDstPort().getPortNumber(), physicalLinks);
		Collection<OVXFlowMod> flows = this.getSrcPort().getParentSwitch().getFlowTable().getFlowTable();
		for (OVXFlowMod fe : flows) {
			for(OFAction act : fe.getActions()) {
				if (act.getType() == OFActionType.OUTPUT && 
						fe.getMatch().getInputPort() == this.getSrcPort().getPortNumber() && 
						((OFActionOutput) act).getPort() == this.getDstPort().getPortNumber()) {
					log.info("Virtual network {}, switch {}, route {}: switch fm {}", this.getTenantId(), 
							this.getSrcPort().getParentSwitch().getSwitchName(), this.getRouteId(), fe);
					counter++;
					this.generateRouteFMs(fe.clone());
					this.generateFirstFM(fe.clone());
				}
			}
		}
		log.info("Virtual network {}, switch {}, route {}: {} flow-mod switched to the new path", this.getTenantId(), 
				this.getSrcPort().getParentSwitch().getSwitchName(), this.getRouteId(), counter);
	}

	public void generateRouteFMs(final OVXFlowMod fm) {
		// This list includes all the actions that have to be applied at the end of the route
		final LinkedList<OFAction> outActions = new LinkedList<OFAction>();
		/*
		 * Check the outPort:
		 * 	- if it's an edge, configure the route's last FM to rewrite the IPs 
		 * 		and generate the route FMs
		 * 	- if it's a link:
		 * 		- retrieve the link
		 * 		- generate the link FMs
		 * 		- configure the route's last FM to rewrite the MACs
		 * 		- generate the route FMs
		 */
		if (this.getDstPort().isEdge()) {
			outActions.addAll(IPMapper.prependUnRewriteActions(fm.getMatch()));
		} else {
			final OVXLink link = this.getDstPort().getLink().getOutLink();
			Integer linkId = link.getLinkId();
			Integer flowId = 0;
			try {
				flowId = OVXMap.getInstance().getVirtualNetwork(this.getTenantId()).getFlowManager()
						.storeFlowValues(fm.getMatch().getDataLayerSource(),
								fm.getMatch().getDataLayerDestination());
				link.generateLinkFMs(fm.clone(), flowId);
				outActions.addAll(new OVXLinkUtils(this.getTenantId(), linkId, flowId).setLinkFields());
			} catch (IndexOutOfBoundException e) {
				log.error("Too many host to generate the flow pairs in this virtual network {}. "
						+ "Dropping flow-mod {} ", this.getTenantId(), fm);
			} catch (NetworkMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/*
		 * If the packet has L3 fields (e.g. NOT ARP), change the packet match:
		 * 1) change the fields where the physical ips are stored
		 */
		if (fm.getMatch().getDataLayerType() == Ethernet.TYPE_IPv4)
			IPMapper.rewriteMatch(this.getSrcPort().getTenantId(), fm.getMatch());

		/*
		 * Get the list of physical links mapped to this virtual link,
		 * in REVERSE ORDER
		 */
		PhysicalPort inPort = null;
		PhysicalPort outPort = null;
		fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);

		final SwitchRoute route = ((OVXBigSwitch) this.getSrcPort()
				.getParentSwitch()).getRoute(this.getSrcPort(), this.getDstPort());
		LinkedList<PhysicalLink> reverseLinks = new LinkedList<>();
		try {
			for (final PhysicalLink phyLink : OVXMap.getInstance().getRoute(route))
				reverseLinks.add(new PhysicalLink(phyLink.getDstPort(), phyLink.getSrcPort()));
		} catch (LinkMappingException e) {
			log.warn("Could not fetch route : {}", e);
			return;
		}
		Collections.reverse(reverseLinks);

		for (final PhysicalLink phyLink : reverseLinks) {
			if (outPort != null) {
				inPort = phyLink.getSrcPort();
				fm.getMatch().setInputPort(inPort.getPortNumber());
				fm.setLengthU(OFFlowMod.MINIMUM_LENGTH
						+ OFActionOutput.MINIMUM_LENGTH);
				fm.setActions(Arrays.asList((OFAction) new OFActionOutput(
						outPort.getPortNumber(), (short) 0xffff)));
				phyLink.getSrcPort().getParentSwitch().sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
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
				outActions.add(new OFActionOutput(this.getDstPort()
						.getPhysicalPortNumber(), (short) 0xffff));
				fm.setActions(outActions);
				for (final OFAction act : outActions) {
					actLenght += act.getLengthU();
				}
				fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + actLenght);
				phyLink.getSrcPort().getParentSwitch().sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
				this.log.debug("Sending big-switch route last fm to sw {}: {}",
						phyLink.getSrcPort().getParentSwitch().getName(), fm);
			}
			outPort = phyLink.getDstPort();
		}

		// TODO: With POX we need to put a timeout between this flows and the
		// first flowMod. Check how to solve
		try {
			Thread.sleep(5);
		} catch (final InterruptedException e1) {}
	}

	private void generateFirstFM(OVXFlowMod fm) {
		fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		final List<OFAction> approvedActions = new LinkedList<OFAction>();
		if (this.getSrcPort().isLink()) {
			OVXPort dstPort = null;
			try {
				dstPort = OVXMap.getInstance().getVirtualNetwork(this.getTenantId()).getNeighborPort(this.getSrcPort());
			} catch (NetworkMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			final OVXLink link = this.getSrcPort().getLink().getOutLink();
			Integer flowId = 0;
			if (link != null) {
				try {
					flowId = OVXMap.getInstance().getVirtualNetwork(this.getTenantId()).getFlowManager().
							getFlowId(fm.getMatch().getDataLayerSource(), fm.getMatch().getDataLayerDestination());
				} catch (NetworkMappingException e) {
					log.warn("SwitchRoute. Error retrieving the network with id {} for flowMod {}. Dropping packet...", 
							this.getTenantId(), fm);
					return;
				} catch (DroppedMessageException e) {
					log.warn("SwitchRoute. Error retrieving flowId in network with id {} for flowMod {}. Dropping packet...",
							this.getTenantId(), fm);
					return;
				}
				OVXLinkUtils lUtils = new OVXLinkUtils(this.getTenantId(), link.getLinkId(), flowId);
				lUtils.rewriteMatch(fm.getMatch());
				IPMapper.rewriteMatch(this.getTenantId(), fm.getMatch());
				approvedActions.addAll(lUtils.unsetLinkFields());
			} else {
				this.log.warn(
						"Cannot retrieve the virtual link between ports {} {}. Dropping packet...",
						dstPort, this.getSrcPort());
				return;
			}
		}
		else {
			approvedActions.addAll(IPMapper.prependRewriteActions(this.getTenantId(), fm.getMatch()));
		}

		fm.getMatch().setInputPort(this.getSrcPort().getPhysicalPortNumber());

		//add the output action with the physical outPort (srcPort of the route)
		if (this.getSrcPort().getPhysicalPortNumber() != this.getPathSrcPort().getPortNumber())
			approvedActions.add(new OFActionOutput(this.getPathSrcPort().getPortNumber()));
		else 
			approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));

		fm.setCommand(OFFlowMod.OFPFC_MODIFY);
		fm.setActions(approvedActions);
		int actLenght = 0;
		for (final OFAction act : approvedActions) {
			actLenght += act.getLengthU();
		}
		fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + actLenght);
		this.getSrcSwitch().sendMsg(fm, this.getSrcSwitch());
		this.log.debug("Sending big-switch route first fm to sw {}: {}", this.getSrcSwitch().getName(), fm);
	}

	public void register() {
		DBManager.getInstance().save(this);
	}

	@Override
	public void unregister() {
		this.srcPort.getParentSwitch().getMap().removeRoute(this);
	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.tenantid);
		return index;
	}

	@Override
	public String getDBKey() {
		return SwitchRoute.DB_KEY;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		try {
			Map<String, Object> dbObject = new HashMap<String, Object>();
			dbObject.put(TenantHandler.DPID, this.dpid);
			dbObject.put(TenantHandler.SRC_PORT, this.srcPort.getPortNumber());
			dbObject.put(TenantHandler.DST_PORT, this.dstPort.getPortNumber());
			dbObject.put(TenantHandler.PRIORITY, this.priority);
			dbObject.put(TenantHandler.ROUTE, this.routeId);
			// Build path list
			List<PhysicalLink> links = OVXMap.getInstance().getRoute(this);
			List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
			for (PhysicalLink link: links) {
				Map obj = link.getDBObject();
				// Physical link id's are meaningless when restarting OVX
				obj.remove(TenantHandler.LINK);
				path.add(obj);
			}
			dbObject.put(TenantHandler.PATH, path);
			return dbObject;
		} catch (LinkMappingException e) {
			return null;
		}
	}


	/**
	 * Tries to switch this route to a backup path, and updates mappings to "correct" 
	 * string of PhysicalLinks to use for this SwitchRoute.  
	 * @param plink the failed PhysicalLink
	 * @return true if successful
	 */
	public boolean tryRecovery(PhysicalLink plink) {
		log.info("Try recovery for virtual network {} big-switch {} internal route {} between ports ({},{}) in virtual network {} ",
				this.getTenantId(), 
				this.getSrcPort().getParentSwitch().getSwitchName(), this.routeId, this.getSrcPort().getPortNumber(),
				this.getDstPort().getPortNumber(), this.getTenantId());
		if (this.backupRoutes.size() > 0) {
			try {
				this.unusableRoutes.put(this.getPriority(), OVXMap.getInstance().getRoute(this));
			} catch (LinkMappingException e) {
				log.warn("No physical Links mapped to SwitchRoute? : {}", e);
				return false;
			}
			int index = this.backupRoutes.size()-1;
			byte priority = (byte) this.backupRoutes.keySet().toArray()[index];
			List<PhysicalLink> phyLinks = this.backupRoutes.get(priority);
			this.switchPath(phyLinks, priority);
			return true;
		}
		else return false;
	}
	
	public HashSet<PhysicalLink> getLinks() {
		
		HashSet<PhysicalLink> list = new HashSet<PhysicalLink>();
		try {
			list.addAll(OVXMap.getInstance().getRoute(this));
		} catch (LinkMappingException e) {
			log.warn("Unable to fetch primary route : {}", e.getMessage());
		}
		for (List<PhysicalLink> links : backupRoutes.values())
			list.addAll(links);
		
		return list;
			
	}

}
