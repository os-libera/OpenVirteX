package net.onrc.openvirtex.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.DPIDandPortPair;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Component that creates a previously stored virtual network when all required switches/links are online.
 */

public class OVXNetworkManager {

	private Map<String, Object> vnet;
	private Integer tenantId;
	// Set of offline and online physical switches 
	private Set<Long> offlineSwitches;
	private Set<Long> onlineSwitches;
	// Set of offline and online physical links identified as (dpid, port)-pair
	private Set<DPIDandPortPair> offlineLinks;
	private Set<DPIDandPortPair> onlineLinks;
	
	private static Logger log = LogManager.getLogger(OVXNetworkManager.class
			.getName());

	public OVXNetworkManager(Map<String, Object> vnet) throws IndexOutOfBoundException, DuplicateIndexException {
		this.vnet = vnet;
		this.tenantId = (Integer) vnet.get(TenantHandler.TENANT);
		this.offlineSwitches = new HashSet<Long>(); 
		this.onlineSwitches = new HashSet<Long>();
		this.offlineLinks = new HashSet<DPIDandPortPair>();
		this.onlineLinks = new HashSet<DPIDandPortPair>();
	}
	
	public Integer getTenantId() {
		return this.tenantId;
	}
	
	public Integer getSwitchCount() {
		return this.offlineSwitches.size() + this.onlineSwitches.size();
	}
	
	public Integer getLinkCount() {
		return this.offlineLinks.size() + this.onlineLinks.size();
	}

	/**
	 * Register switch identified by dpid, ensuring the virtual network
	 * is spawned only after the switch is online.
	 *  
	 * @param dpid
	 */
	public void registerSwitch(final Long dpid) {
		this.offlineSwitches.add(dpid);
	}
	
	/**
	 * Register link identified by key, ensuring the virtual network
	 * is spawned only after the link is online.
	 *  
	 * @param dpp
	 */
	public void registerLink(final DPIDandPortPair dpp) {
		this.offlineLinks.add(dpp);
	}
	
	/**
	 * Change switch from offline to online state
	 * @param key Unique datapath id
	 */
	public synchronized void setSwitch(final Long dpid) {
		this.offlineSwitches.remove(dpid);
		this.onlineSwitches.add(dpid);
	}
	
	/**
	 * Change switch from online to offline state
	 * @param key Unique datapath id
	 */
	public synchronized void unsetSwitch(final Long dpid) {
		this.offlineSwitches.add(dpid);
		this.onlineSwitches.remove(dpid);
	}
	
	/**
	 * Change link from offline to single direction state, or from single direction to online.
	 * Create and start virtual network if all links and switches are online.
	 * @param key Unique link
	 */
	public synchronized void setLink(final DPIDandPortPair dpp) {
		if (this.offlineLinks.contains(dpp)) {
			this.offlineLinks.remove(dpp);
			this.onlineLinks.add(dpp);
			if (this.offlineSwitches.isEmpty() && this.offlineLinks.isEmpty())
				this.createNetwork();
		}
	}

	/**
	 * Change link from online to offline state
	 * @param key Unique link
	 */
	public synchronized void unsetLink(final DPIDandPortPair dpp) {
		if (this.onlineLinks.contains(dpp)) {
			this.onlineLinks.remove(dpp);
			this.offlineLinks.add(dpp);
		}
	}

	/**
	 * Convert path in db map format to list of physical links
	 * @param path
	 * @return
	 */
	private List<PhysicalLink> pathToPhyLinkList(List<Map> path) {
		// Build list of physical links
		final List<PhysicalLink> result = new ArrayList<PhysicalLink>();
		for (Map<String, Object> hop: path) {
			// Get src/dst dpid & port number from map
			final Long sDpid = (Long) hop.get(TenantHandler.SRC_DPID);
			final Short sPort = ((Integer) hop.get(TenantHandler.SRC_PORT)).shortValue();
			final Long dDpid = (Long) hop.get(TenantHandler.DST_DPID);
			final Short dPort = ((Integer) hop.get(TenantHandler.DST_PORT)).shortValue();
			
			// Get physical switch instances of end points
			// TODO: what if any of the elements have gone down in the meantime? 
			final PhysicalSwitch src = PhysicalNetwork.getInstance().getSwitch(sDpid);
			final PhysicalSwitch dst = PhysicalNetwork.getInstance().getSwitch(dDpid);
			
			// Get physical link instance
			final PhysicalLink phyLink = PhysicalNetwork.getInstance()
					.getLink(src.getPort(sPort), dst.getPort(dPort));
			
			result.add(phyLink);
		}
		return result;
	}
	
	/**
	 * Creates OVX network and elements based on persistent storage, boots network afterwards. 
	 * TODO: proper error handling (roll-back?)
	 */
	private void createNetwork() {
		OVXNetworkManager.log.info("Virtual network {} ready for boot", this.tenantId);
		// Create OVX network
		final Integer tenantId = (Integer) this.vnet.get(TenantHandler.TENANT);
		final String protocol = (String) this.vnet.get(TenantHandler.PROTOCOL); 
		final String ctrlAddress = (String) this.vnet.get(TenantHandler.CTRLHOST);
		final Integer ctrlPort = (Integer) this.vnet.get(TenantHandler.CTRLPORT);
		final Integer network = (Integer) this.vnet.get(TenantHandler.NETADD);		
		final IPAddress addr = new OVXIPAddress(network, -1);
		final Short netMask = ((Integer) this.vnet.get(TenantHandler.NETMASK)).shortValue();
		OVXNetwork virtualNetwork;
		try {
			virtualNetwork = new OVXNetwork(tenantId, protocol, ctrlAddress, ctrlPort, addr, netMask);
		} catch (IndexOutOfBoundException e) {
			OVXNetworkManager.log.error("Error recreating virtual network {} from database", tenantId);
			return;
		}
		virtualNetwork.register();

		// Create OVX switches
		final List<Map<String, Object>> switches = (List<Map<String, Object>>) this.vnet.get(Switch.DB_KEY);
		if (switches != null) {
		for (Map<String, Object> sw: switches) {
			List<Long> dpids = (List<Long>) sw.get(TenantHandler.DPIDS);
			long switchId = (long) sw.get(TenantHandler.DPID);
			try {
				virtualNetwork.createSwitch(dpids, switchId);
			} catch (IndexOutOfBoundException e) {
				OVXNetworkManager.log.error("Error recreating virtual switch {} from database", switchId);
				return;
			}
		}
		}

		// Create OVX ports
		final List<Map<String, Object>> ports = (List<Map<String, Object>>) this.vnet.get(Port.DB_KEY);
		if (ports != null) {
		for (Map<String, Object> port: ports) {
			long physicalDpid = (Long) port.get(TenantHandler.DPID);
			short portNumber = ((Integer) port.get(TenantHandler.PORT)).shortValue();
			short vportNumber = ((Integer) port.get(TenantHandler.VPORT)).shortValue();
			try {
				virtualNetwork.createPort(physicalDpid, portNumber, vportNumber);
			} catch (IndexOutOfBoundException e) {
				OVXNetworkManager.log.error("Error recreating virtual port {} from database", vportNumber);
				return;
			}
		}
		}
		

// DISABLED FOR NOW AS OVXBIGSWITCH ONLY SUPPORTS INTERNAL ROUTING		
//		// Create OVX big switch routes
//		final List<Map<String, Object>> routes = (List<Map<String, Object>>) this.vnet.get(SwitchRoute.DB_KEY);
//		// List of created routeId's per switch
//		final Map<Long, List<Integer>> routeIds = new HashMap<Long, List<Integer>>();
//		if (routes != null) {
//		for (Map<String, Object> route: routes) {
//			long dpid = (Long) route.get(TenantHandler.DPID);
//			short srcPort = ((Integer) route.get(TenantHandler.SRC_PORT)).shortValue();
//			short dstPort = ((Integer) route.get(TenantHandler.DST_PORT)).shortValue();
//			byte priority = ((Integer) route.get(TenantHandler.PRIORITY)).byteValue();
//			int routeId = (Integer) route.get(TenantHandler.ROUTE);
//			// Maintain id's of routes per switch so we don't create reverse
//			List<Integer> visited = routeIds.get(dpid);
//			if (visited == null) {
//				visited = new ArrayList<Integer>();
//				routeIds.put(dpid, visited);
//			}
//			if (visited.contains(routeId))
//				continue;
//			else
//				visited.add(routeId);
//
//			List<Map> path = (List<Map>) route.get(TenantHandler.PATH);
//			List<PhysicalLink> physicalLinks = this.pathToPhyLinkList(path);
//			
//			try {
//				virtualNetwork.connectRoute(dpid, srcPort, dstPort, physicalLinks, priority, routeId);
//			} catch (IndexOutOfBoundException e) {
//				OVXNetworkManager.log.error("Error recreating virtual switch route {} from database", routeId);
//				return;
//			}
//		}
//		}
		
		// Create OVX links
		final List<Map<String, Object>> links = (List<Map<String, Object>>) this.vnet.get(Link.DB_KEY);
		// Maintain link id's of virtual links we have created - ensure reverse link is not created again
		final List<Integer> linkIds = new ArrayList<Integer>();
		if (links != null) {
		for (Map<String, Object> link: links) {
			// Skip link if we already handled the reverse
			Integer linkId = (Integer) link.get(TenantHandler.LINK);
			if (linkIds.contains(linkId))
				continue;
			else
				linkIds.add(linkId);
			// Obtain virtual src and dst dpid/port, priority
			Long srcDpid = (Long) link.get(TenantHandler.SRC_DPID);
			Short srcPort = ((Integer) link.get(TenantHandler.SRC_PORT)).shortValue();
			Long dstDpid = (Long) link.get(TenantHandler.DST_DPID);
			Short dstPort = ((Integer) link.get(TenantHandler.DST_PORT)).shortValue();
			Byte priority = ((Integer) link.get(TenantHandler.PRIORITY)).byteValue();
			
			// Build list of physical links
			List<Map> path = (List<Map>) link.get(TenantHandler.PATH);
			List<PhysicalLink> physicalLinks = this.pathToPhyLinkList(path);
			
			// Create virtual link
			try {
				virtualNetwork.connectLink(srcDpid, srcPort, dstDpid, dstPort, physicalLinks, priority, linkId);
			} catch (IndexOutOfBoundException e) {
				OVXNetworkManager.log.error("Error recreating virtual link {} from database", linkId);
				return;
			}
		}
		}

		// Connect hosts
		final List<Map<String, Object>> hosts = (List<Map<String, Object>>) this.vnet.get(Host.DB_KEY);
		if (hosts != null) {
		for (Map<String, Object> host: hosts) {
			final long dpid = (Long) host.get(TenantHandler.DPID);
			final short port = ((Integer) host.get(TenantHandler.PORT)).shortValue();
			final MACAddress macAddr = MACAddress.valueOf((Long) host.get(TenantHandler.MAC));
			final int hostId = (Integer) host.get(TenantHandler.HOST);
			try {
				virtualNetwork.connectHost(dpid, port, macAddr, hostId);
			} catch (IndexOutOfBoundException e) {
				OVXNetworkManager.log.error("Failed to create host {}", hostId);
			}
		}
		}

		// Start network
		virtualNetwork.boot();
	}
}
