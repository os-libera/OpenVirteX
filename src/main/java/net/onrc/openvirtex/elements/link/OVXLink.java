/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.TreeMap;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Resilient;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
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
import org.openflow.protocol.OFPhysicalPort.OFPortState;
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
public class OVXLink extends Link<OVXPort, OVXSwitch> implements Resilient {
	
	enum LinkState {
		INIT {
			protected void initialize(OVXLink link) {
				log.debug("Initializing link {}", link);
				link.srcPort.setOutLink(link);
				link.dstPort.setInLink(link);

				try {
					link.map.getVirtualNetwork(link.tenantId).addLink(link);
				} catch (NetworkMappingException e) {
					log.warn("No OVXNetwork associated with this link [{}-{}]", 
							link.srcPort.toAP(), link.dstPort.toAP());
				}
			}
			
			protected void register(final OVXLink link, 
					final List<PhysicalLink> physicalLinks, 
					byte priority) {	
				log.debug("registering link {}", link);
				link.state = LinkState.INACTIVE;
			
				//register the primary link in the map
				link.map.removeVirtualLink(link);
				link.map.addLinks(physicalLinks, link);
				link.backupLink(physicalLinks, priority);
				link.srcPort.setEdge(false);
				link.dstPort.setEdge(false);
				DBManager.getInstance().save(link);
			}
		},
		INACTIVE {
			protected boolean boot(OVXLink link) {
				log.debug("enabling link {}", link);
				link.state = LinkState.ACTIVE;
				int linkup = ~OFPortState.OFPPS_LINK_DOWN.getValue();
				link.srcPort.setState(link.srcPort.getState() & linkup);
				link.dstPort.setState(link.dstPort.getState() & linkup);
				return true;
			}
			
			protected void unregister(OVXLink link) {
				log.debug("unregistering link {}", link);
				link.state = LinkState.STOPPED;
				
				try {
					setEndStates(link, OFPortState.OFPPS_LINK_DOWN.getValue());
					link.srcPort.setEdge(true);
					link.srcPort.setOutLink(null);
					link.dstPort.setEdge(true);
					link.dstPort.setInLink(null);
					link.map.removeVirtualLink(link);
					link.map.getVirtualNetwork(link.tenantId).removeLink(link);
					DBManager.getInstance().remove(link);
				} catch (NetworkMappingException e) {
					log.warn("[unregister()]: could not remove this link from map \n{}", e.getMessage());
				}
			}
			
			public boolean tryRevert(OVXLink vlink, PhysicalLink plink) {
				if (vlink.unusableLinks.isEmpty()) {
					return false;
				}
				synchronized (vlink.unusableLinks) {
					Iterator<Byte> it = vlink.unusableLinks.descendingKeySet().iterator();
					while (it.hasNext()) {
						Byte curPriority = it.next();
						if (vlink.unusableLinks.get(curPriority).contains(plink)) {
							log.debug("Reactivate all inactive paths for virtual link {} in virtual network {} ", vlink.linkId, vlink.tenantId);
	
							if (U8.f(vlink.getPriority()) >= U8.f(curPriority)) {
								vlink.backupLinks.put(curPriority, vlink.unusableLinks.get(curPriority));
							}
							else {
	
								try {
									List<PhysicalLink> backupLinks = new ArrayList<>(vlink.map.getPhysicalLinks(vlink));
									Collections.copy(backupLinks, vlink.map.getPhysicalLinks(vlink));
									vlink.backupLinks.put(vlink.getPriority(), backupLinks);
									vlink.switchPath(vlink.unusableLinks.get(curPriority), curPriority);
								} catch (LinkMappingException e) {
									log.warn("No physical Links mapped to SwitchRoute? : {}", e);
									return false;
								}
							}
							it.remove();
						}
					}
				}
				vlink.backupLinks.remove(vlink.priority);
				vlink.boot();
				return true;
			}
			
			public void generateFMs(OVXLink link, OVXFlowMod fm, Integer flowId) {
				link.generateFlowMods(fm, flowId);
			}
		},
		ACTIVE {
			protected boolean teardown(OVXLink link) {
				log.debug("disabling link {}", link);
				link.state = LinkState.INACTIVE;
				
				setEndStates(link, OFPortState.OFPPS_LINK_DOWN.getValue());
				return true;
			}
			
			public boolean tryRecovery(OVXLink vlink, PhysicalLink plink) {
				log.debug("Try recovery for virtual link {} [id={}, tID={}]", 
						vlink, vlink.linkId, vlink.tenantId);
				/* store broken link to force re-initialization when we tryRevert().*/
				try {
					List<PhysicalLink> unusableLinks = new ArrayList<>(vlink.map.getPhysicalLinks(vlink));
					Collections.copy(unusableLinks, vlink.map.getPhysicalLinks(vlink));
					vlink.unusableLinks.put(vlink.getPriority(), unusableLinks);
				} catch (LinkMappingException e) {
					log.warn("No physical Links mapped to OVXLink? : {}", e);
					return false;
				}
				if (vlink.backupLinks.size() > 0) {
					byte priority = vlink.backupLinks.lastKey();
					List<PhysicalLink> phyLinks = vlink.backupLinks.get(priority);
					vlink.switchPath(phyLinks, priority);
					vlink.backupLinks.remove(priority);
					return true;
				}
				else return false;
			}
			
			public void generateFMs(OVXLink link, OVXFlowMod fm, Integer flowId) {
				link.generateFlowMods(fm, flowId);
			}	
		},
		STOPPED;
		
		protected void initialize(final OVXLink link) {
			log.debug("Cannot initialize link {} while status={}", link, link.state);
		}
		
		protected void register(final OVXLink link, 
				final List<PhysicalLink> physicalLinks, 
				byte priority) {	
			log.debug("Cannot register link {} while status={}", link, link.state);
		}
		
		protected boolean boot(OVXLink link) {
			log.debug("Cannot boot link {} while status={}", link, link.state);
			return false;
		}
		
		protected boolean teardown(OVXLink link) {
			log.debug("Cannot teardown link {} while status={}", link, link.state);
			return false;
		}
		
		protected void unregister(OVXLink link) {
			log.debug("Cannot unregister link {} while status={}", link, link.state);
		}
		
		private static void setEndStates(OVXLink link, int nstate) {
			link.srcPort.setState(link.srcPort.getState() | nstate);
			link.dstPort.setState(link.dstPort.getState() | nstate);
		}

		public boolean tryRecovery(OVXLink ovxLink, PhysicalLink plink) {
			return false;
		}

		public boolean tryRevert(OVXLink ovxLink, PhysicalLink plink) {
			return false;
		}

		public void generateFMs(OVXLink ovxLink, OVXFlowMod fm, Integer flowId) {}
	}
	
	static Logger                log = LogManager.getLogger(OVXLink.class.getName());

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

	private LinkState state;
	
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
		this.state = LinkState.INIT;
		this.linkId = linkId;
		this.tenantId = tenantId;
		this.backupLinks = new TreeMap<>();
		this.unusableLinks = new TreeMap<>();
		this.priority = (byte) 0;
		this.alg = alg;
		this.map = OVXMap.getInstance();
		/* If SPF routing, let RoutingAlgorithm initialize() link, else do it here */
		if (this.alg.getRoutingType() != RoutingType.NONE) { 
			this.alg.getRoutable().setLinkPath(this);
		} else {
			this.initialize();
		}
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
	 * Pre-registration method independent of this OVXLink's Physical dependencies. 
	 * Must be called before register().  
	 */
	public void initialize() {
		this.state.initialize(this);
	}

	/**
	 * Register mapping between virtual link and physical path
	 * 
	 * @param physicalLinks
	 * @param priority 
	 */
	public void register(final List<PhysicalLink> physicalLinks, byte priority) {
		this.state.register(this, physicalLinks, priority);
	}
	
	private void backupLink(final List<PhysicalLink> physicalLinks, byte priority) {
		if (U8.f(this.getPriority()) >= U8.f(priority)) {
			this.backupLinks.put(priority, physicalLinks);
			log.debug("Add virtual link {} backup path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
					this.getLinkId(), U8.f(priority), this.srcPort.toAP(), this.dstPort.toAP(), 
					this.getTenantId(), physicalLinks);
		} else {
			try {
				this.backupLinks.put(this.getPriority(), map.getPhysicalLinks(this));
				log.debug("Replace virtual link {} with a new primary path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
						this.getLinkId(), U8.f(priority), this.srcPort.toAP(), this.dstPort.toAP(), 
						this.getTenantId(), physicalLinks);
				log.info("Switch all existing flow-mods crossing the virtual link {} between ports ({}-{}) to new path", 
						this.getLinkId(), this.getSrcPort().toAP(), this.getDstPort().toAP());
			}
			catch (LinkMappingException e) {
				log.debug("Create virtual link {} primary path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
						this.getLinkId(), U8.f(priority), this.srcPort.toAP(), this.dstPort.toAP(), 
						this.getTenantId(), physicalLinks);
			}
			//TODO this should be boot()
			this.switchPath(physicalLinks, priority);			
		}	
	}

	@Override
	public void unregister() {
		this.state.unregister(this);
	}    

	/**
	 * Disables this OVXLink temporarily. 
	 */
	public boolean tearDown() {
		return this.state.teardown(this);
	}

	private void switchPath(List<PhysicalLink> physicalLinks, byte priority) {
		
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
							
							OVXFlowMod fm = fe.clone();
							fm.setCookie(((OVXFlowTable) this.getSrcPort().getParentSwitch().getFlowTable()).getCookie(fe, true));
							this.generateLinkFMs(fm, flowId);
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
		Map<String, Object> dbObject = super.getDBObject(); 
		dbObject.put(TenantHandler.LINK, this.linkId);
		dbObject.put(TenantHandler.PRIORITY, this.priority);
		dbObject.put(TenantHandler.ALGORITHM, this.alg.getRoutingType().getValue());
		dbObject.put(TenantHandler.BACKUPS, this.alg.getBackups());
		try {
			// Build path list
			List<PhysicalLink> links = map.getPhysicalLinks(this);
			List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
			for (PhysicalLink link: links) {
				// Physical link id's are meaningless when restarting OVX
				Map<String, Object> obj = link.getDBObject();
				obj.remove(TenantHandler.LINK);
				path.add(obj);
			}
			dbObject.put(TenantHandler.PATH, path);
		} catch (LinkMappingException e) {
			;
		}
		return dbObject;
	}

	/**
	 * Push the flow-mod to all the middle point of a virtual link
	 * 
	 * @param the original flow mod 
	 * @param the flow identifier
	 * @param the source switch
	 */
	public void generateLinkFMs(final OVXFlowMod fm, final Integer flowId) {
		this.state.generateFMs(this, fm, flowId);
	}
	
	/**
	 * Helper function that does FlowMod generation. 
	 * 
	 * @param the original flow mod
	 * @param the flow identifier
	 * @param the source switch
	 */
	private void generateFlowMods(final OVXFlowMod fm, final Integer flowId) {
		/*
		 * Change the packet match:
		 * 1) change the fields where the virtual link info are stored
		 * 2) change the fields where the physical ips are stored
		 */
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
			for (final PhysicalLink phyLink : OVXMap.getInstance().getPhysicalLinks(this)) {
				PhysicalLink nlink = new PhysicalLink(phyLink.getDstPort(), phyLink.getSrcPort());
				plinks.add(nlink);
				nlink.boot();
			}
		} catch (LinkMappingException e) {
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
				log.debug(
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
	public boolean tryRecovery(Component plink) {
		return this.state.tryRecovery(this, (PhysicalLink)plink);
	}

	/**
	 * Attempts to switch this link back to the original path.
	 * @param plink
	 * @return true for success, false otherwise. 
	 */
	public boolean tryRevert(Component plink) {
		return this.state.tryRevert(this, (PhysicalLink)plink);
	}

	@Override
	public boolean boot() {
		return this.state.boot(this);
	}

}
