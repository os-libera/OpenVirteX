/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.routing.RoutingAlgorithms.RoutingType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.util.U8;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The Class OVXLink.
 * 
 */
public class OVXLink extends Link<OVXPort, OVXSwitch> {
	Logger                log = LogManager.getLogger(OVXLink.class.getName());

	/** The link id. */


	@SerializedName("linkId")
	@Expose
	private final Integer linkId;

	/** The tenant id. */
	@SerializedName("tenantId")
	@Expose
	private final Integer tenantId;

	private byte priority;

	private RoutingAlgorithms alg;

	private final TreeMap<Byte, List<PhysicalLink>> backupLinks;
	private final TreeMap<Byte, List<PhysicalLink>> unusableLinks;

	private Mappable      map = null;


	/**
	 * Instantiates a new virtual link.
	 * 
	 * @param linkId
	 *            link id
	 * @param tenantId
	 *            tenant id
	 * @param srcPort
	 *            virtual source port
	 * @param dstPort
	 *            virtual destination port
	 * @param priority 
	 * @throws PortMappingException 
	 */
	public OVXLink(final Integer linkId, final Integer tenantId,
			final OVXPort srcPort, final OVXPort dstPort, RoutingAlgorithms alg) 
					throws PortMappingException {
		super(srcPort, dstPort);
		this.linkId = linkId;
		this.tenantId = tenantId;
		srcPort.setOutLink(this);
		dstPort.setInLink(this);
		this.backupLinks = new TreeMap<>();
		this.unusableLinks = new TreeMap<>();
		this.priority = (byte) 0;
		this.alg = alg;
		this.map = OVXMap.getInstance();
		if (this.alg.getRoutingType() != RoutingType.NONE) 
			this.alg.getRoutable().setLinkPath(this);
		this.srcPort.getPhysicalPort().removeOVXPort(this.srcPort);
		this.srcPort.getPhysicalPort().setOVXPort(this.srcPort);
	}

	/**
	 * Gets the link id.
	 * 
	 * @return the link id
	 */
	public Integer getLinkId() {
		return this.linkId;
	}

	/**
	 * Gets the tenant id.
	 * 
	 * @return the tenant id
	 */
	public Integer getTenantId() {
		return this.tenantId;
	}

	public byte getPriority() {
		return priority;
	}


	public void setPriority(byte priority) {
		this.priority = priority;
	}

	/**
	 * Gets the alg.
	 * 
	 * @return the alg
	 */
	public RoutingAlgorithms getAlg() {
		return this.alg;
	}

	public void setAlg(final RoutingAlgorithms alg) {
		this.alg = alg;
	}


	/**
	 * Register mapping between virtual link and physical path
	 * 
	 * @param physicalLinks
	 * @param priority 
	 */
	public void register(final List<PhysicalLink> physicalLinks, byte priority) {
		if (U8.f(this.getPriority()) >= U8.f(priority)) {
			this.backupLinks.put(priority, physicalLinks);
			log.debug("Add virtual link {} backup path (priority {}) between ports {}/{} - {}/{} in virtual network {}. Path: {}",
					this.getLinkId(), U8.f(priority), this.getSrcSwitch()
					.getSwitchName(), this.srcPort.getPortNumber(), this.getDstSwitch().getSwitchName(), this.dstPort.getPortNumber(), 
					this.getTenantId(), physicalLinks);
		}
		else {
			try {
				this.backupLinks.put(this.getPriority(), map.getPhysicalLinks(this));
				log.debug("Replace virtual link {} with a new primary path (priority {}) between ports {}/{} - {}/{} in virtual network {}. Path: {}",
						this.getLinkId(), U8.f(priority), this.getSrcSwitch()
						.getSwitchName(), this.srcPort.getPortNumber(), this.getDstSwitch().getSwitchName(), this.dstPort.getPortNumber(), 
						this.getTenantId(), physicalLinks);
				log.info("Switch all existing flow-mods crossing the virtual link {} between ports ({}/{},{}/{}) to new path", 
						this.getLinkId(), this.getSrcSwitch().getSwitchName(), this.getSrcPort().getPortNumber(),
						this.getDstSwitch().getSwitchName(), this.getDstPort().getPortNumber());
			}
			catch (LinkMappingException e) {
				log.debug("Create virtual link {} primary path (priority {}) between ports {}/{} - {}/{} in virtual network {}. Path: {}",
						this.getLinkId(), U8.f(priority), this.getSrcSwitch()
						.getSwitchName(), this.srcPort.getPortNumber(), this.getDstSwitch().getSwitchName(), this.dstPort.getPortNumber(), 
						this.getTenantId(), physicalLinks);
			}
			this.switchPath(physicalLinks, priority);			
		}	

		DBManager.getInstance().save(this);
	}

	@Override
	public void unregister() {

		try {
			DBManager.getInstance().remove(this);
			this.tearDown();
			map.removeVirtualLink(this);
			map.getVirtualNetwork(this.tenantId).removeLink(this);
		} catch (NetworkMappingException e) {
			log.warn("[unregister()]: could not remove this link from map \n{}", e.getMessage());
		}
	}    

	/**
	 * Disables this OVXLink by disabling its endpoints. 
	 */
	public void tearDown() {
		this.srcPort.tearDown();
		this.dstPort.tearDown();
	}

	public void switchPath(List<PhysicalLink> physicalLinks, byte priority) {
		//register the primary link in the map
		this.srcPort.getParentSwitch().getMap().removeVirtualLink(this);
		this.srcPort.getParentSwitch().getMap().addLinks(physicalLinks, this);
		
		this.setPriority(priority);

		Collection<OVXFlowMod> flows = this.getSrcSwitch().getFlowTable().getFlowTable();
		for (OVXFlowMod fe : flows) {
			for(OFAction act : fe.getActions()) {
				if (act.getType() == OFActionType.OUTPUT) {
					if (((OFActionOutput) act).getPort() == this.getSrcPort().getPortNumber()) {
						try {
							Integer flowId = this.map.getVirtualNetwork(this.tenantId).getFlowManager()
									.storeFlowValues(fe.getMatch().getDataLayerSource(),
											fe.getMatch().getDataLayerDestination());
							this.generateLinkFMs(fe.clone(), flowId);
						} catch (IndexOutOfBoundException e) {
							log.error("Too many host to generate the flow pairs in this virtual network {}. "
									+ "Dropping flow-mod {} ", this.getTenantId(), fe);
						} catch (NetworkMappingException e) {
							log.warn("{}: skipping processing of OFAction", e);
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.tenantId);
		return index;
	}

	@Override
	public String getDBKey() {
		return Link.DB_KEY;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		try {
			Map<String, Object> dbObject = super.getDBObject(); 
			dbObject.put(TenantHandler.LINK, this.linkId);
			dbObject.put(TenantHandler.PRIORITY, this.priority);
			// Build path list
			List<PhysicalLink> links = map.getPhysicalLinks(this);
			List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
			for (PhysicalLink link: links) {
				// Physical link id's are meaningless when restarting OVX
				Map obj = link.getDBObject();
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
	 * Push the flow-mod to all the middle point of a virtual link
	 * 
	 * @param the
	 *            original flow mod
	 * @param the
	 *            flow identifier
	 * @param the
	 *            source switch
	 */
	public void generateLinkFMs(final OVXFlowMod fm, final Integer flowId) {
		/*
		 * Change the packet match:
		 * 1) change the fields where the virtual link info are stored
		 * 2) change the fields where the physical ips are stored
		 */
		fm.setPhysicalCookie();
		final OVXLinkUtils lUtils = new OVXLinkUtils(this.tenantId,
				this.linkId, flowId);
		lUtils.rewriteMatch(fm.getMatch());
		long cookie = tenantId;
		fm.setCookie(cookie << 32);

		if (fm.getMatch().getDataLayerType() == Ethernet.TYPE_IPv4)
			IPMapper.rewriteMatch(this.tenantId, fm.getMatch());

		/*
		 * Get the list of physical links mapped to this virtual link,
		 * in REVERSE ORDER
		 */
		PhysicalPort inPort = null;
		PhysicalPort outPort = null;
		fm.setBufferId(OVXPacketOut.BUFFER_ID_NONE);
		fm.setCommand(OFFlowMod.OFPFC_MODIFY);
		List<PhysicalLink> plinks = new LinkedList<PhysicalLink>();
		try {
			final OVXLink link = this.map.getVirtualNetwork(this.tenantId)
					.getLink(this.srcPort, this.dstPort); 
			for (final PhysicalLink phyLink : OVXMap.getInstance().getPhysicalLinks(link))
				plinks.add(new PhysicalLink(phyLink.getDstPort(), phyLink.getSrcPort()));

		} catch (LinkMappingException | NetworkMappingException e) {
			log.warn("No physical Links mapped to OVXLink? : {}", e);
			return;
		}

		Collections.reverse(plinks);

		for (final PhysicalLink phyLink : plinks) {
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
						"Sending virtual link intermediate fm to sw {}: {}",
						phyLink.getSrcPort().getParentSwitch().getSwitchName(), fm);

			}
			outPort = phyLink.getDstPort();
		}
		// TODO: With POX we need to put a timeout between this flows and the
		// first flowMod. Check how to solve
		try {
			Thread.sleep(5);
		} catch (final InterruptedException e) {
		}
	}

	/**
	 * Tries to switch link to a backup path, and updates mappings to "correct" 
	 * string of PhysicalLinks to use for this OVXLink.  
	 * @param plink the failed PhysicalLink
	 * @return true if successful
	 */
	public boolean tryRecovery(PhysicalLink plink) {
		log.info("Try recovery for virtual link {} in virtual network {} ", this.linkId, this.tenantId);
		if (this.backupLinks.size() > 0) {
			try {
				List<PhysicalLink> unusableLinks = new ArrayList<>(map.getPhysicalLinks(this));
				Collections.copy(unusableLinks, map.getPhysicalLinks(this));
				this.unusableLinks.put(this.getPriority(), unusableLinks);
			} catch (LinkMappingException e) {
				log.warn("No physical Links mapped to OVXLink? : {}", e);
				return false;
			}
			byte priority = this.backupLinks.lastKey();
			List<PhysicalLink> phyLinks = this.backupLinks.get(priority);
			this.switchPath(phyLinks, priority);
			this.backupLinks.remove(priority);
			return true;
		}
		else return false;
	}

	/**
	 * Attempts to switch this link back to the original path.
	 * @param plink
	 * @return true for success, false otherwise. 
	 */
	public boolean tryRevert(PhysicalLink plink) {
		Iterator<Byte> it = this.unusableLinks.descendingKeySet().iterator();
		while (it.hasNext()) {
			Byte curPriority = it.next();
			if (this.unusableLinks.get(curPriority).contains(plink)) {
				log.info("Reactivate all inactive paths for virtual link {} in virtual network {} ", this.linkId, this.tenantId);
				
				if (U8.f(this.getPriority()) >= U8.f(curPriority)) {
					this.backupLinks.put(curPriority, this.unusableLinks.get(curPriority));
				}
				else {
					
					try {
						List<PhysicalLink> backupLinks = new ArrayList<>(map.getPhysicalLinks(this));
						Collections.copy(backupLinks,map.getPhysicalLinks(this));
						this.backupLinks.put(this.getPriority(), backupLinks);
						this.switchPath(this.unusableLinks.get(curPriority), curPriority);
					} catch (LinkMappingException e) {
						log.warn("No physical Links mapped to SwitchRoute? : {}", e);
						return false;
					}
				}
				it.remove();
			}
		}
		return true;
	}

}
