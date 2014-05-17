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
package net.onrc.openvirtex.elements.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;
import net.onrc.openvirtex.util.MACAddress;
import net.onrc.openvirtex.util.OVXFlowManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import com.google.common.collect.Lists;


/**
 * Virtual networks contain tenantId, controller info, subnet and gateway
 * information. Handles registration of virtual switches and links. Responds to
 * LLDP discovery probes from the controller.
 * 
 */

public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> implements Persistable {
	
	/**
	 * FSM representing the states of a virtual network. 
	 */
	enum NetworkState {
		INIT {
			protected void register(OVXNetwork vnet) {	
				log.debug("registering tenant {}", vnet.tenantId);
				OVXMap.getInstance().addNetwork(vnet);
				DBManager.getInstance().createDoc(vnet);
				vnet.state = NetworkState.INACTIVE;
			}
		},
		INACTIVE {
			protected boolean boot(OVXNetwork vnet) {
				log.debug("booting tenant {}", vnet.tenantId);
				boolean result = true;
				try {
					vnet.flowManager.boot();
				} catch (final IndexOutOfBoundException e) {
					OVXNetwork.log
					.error("Too many host to generate the flow pairs. Tear down the virtual network {}",
							vnet.tenantId);
					return false;
				}
				for (final OVXSwitch sw : vnet.getSwitches()) {
					result &= sw.boot();
				}
				if (result) {
					vnet.state = NetworkState.ACTIVE;	
				}
				return result;
			}
			
			protected void unregister(OVXNetwork vnet) {
				log.debug("unregistering tenant {}", vnet.tenantId);
				DBManager.getInstance().removeDoc(vnet);
				final LinkedList<Long> dpids = new LinkedList<>();
				for (final OVXSwitch virtualSwitch : vnet.getSwitches()) {
					dpids.add(virtualSwitch.getSwitchId());
				}
				OVXSwitch vsw;
				for (final Long dpid : dpids) {
					vsw = vnet.getSwitch(dpid);
					vsw.tearDown();
					vsw.unregister();
				}
				// remove the network from the Map
				OVXMap.getInstance().removeVirtualIPs(vnet.tenantId);
				OVXMap.getInstance().removeNetwork(vnet);
				OpenVirteXController.getTenantCounter().releaseIndex(vnet.tenantId);
				vnet.state = NetworkState.STOPPED;		
			}
		},
		ACTIVE {
			protected boolean teardown(OVXNetwork vnet) {
				log.debug("disabling tenant {}", vnet.tenantId);
				/* tear-down all vswitches. Need to really think about
				 * retries and when states go wonky */
				boolean res = true;
				for (final OVXSwitch vsw : vnet.getSwitches()) {
					res &= vsw.tearDown();
				}
				if (res) {
					vnet.state = NetworkState.INACTIVE;
				} else {
					log.warn("Not all virtual switches have been torn down: "
							+ "Tenant {} can't shut down", vnet.tenantId);
				}
				return res;
			}
			
			@SuppressWarnings("rawtypes")
			protected void handleLLDP(OVXNetwork vnet, OFMessage msg, Switch sw) {
				final OVXPacketOut po = (OVXPacketOut) msg;
				final byte[] pkt = po.getPacketData();
				// Create LLDP response for each output action port
				for (final OFAction action : po.getActions()) {
					try {
						final short portNumber = ((OFActionOutput) action)
								.getPort();
						final OVXPort srcPort = (OVXPort) sw.getPort(portNumber);
						final OVXPort dstPort = vnet.getNeighborPort(srcPort);
						if (dstPort != null) {
							final OVXPacketIn pi = new OVXPacketIn();
							pi.setBufferId(OFPacketOut.BUFFER_ID_NONE);
							// Get input port from pkt_out
							pi.setInPort(dstPort.getPortNumber());
							pi.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
							pi.setPacketData(pkt);
							pi.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + pkt.length));
							dstPort.getParentSwitch().sendMsg(pi, vnet);
						}
					} catch (final ClassCastException c) {
						// ignore non-ActionOutput pkt_out's
					}
				}
			}
		},
		STOPPED;
		
		protected void register(OVXNetwork vnet) {	
			log.debug("Cannot register tenant {} while status={}",
					vnet.tenantId, vnet.state);
		}
		
		protected boolean boot(OVXNetwork vnet) {
			log.debug("Cannot boot tenant {} while status={}",
					vnet.tenantId, vnet.state);
			return false;
		}
		
		protected boolean teardown(OVXNetwork vnet) {
			log.debug("Cannot teardown tenant {} while status={}", 
					vnet.tenantId, vnet.state);
			return false;
		}
		
		protected void unregister(OVXNetwork vnet) {
			log.debug("Cannot unregister tenant {} while status={}", 
					vnet.tenantId, vnet.state);
		}

		@SuppressWarnings("rawtypes")
		protected void handleLLDP(OVXNetwork ovxNetwork, OFMessage msg, Switch sw) {
		}
		
	}
	
	static Logger                                log             = LogManager
			.getLogger(OVXNetwork.class
					.getName());

	private final Integer                        tenantId;
	private final HashSet<String>			 controllerUrls;
	private final IPAddress                      network;
	private final short                          mask;
	private HashMap<IPAddress, MACAddress>       gwsMap;
	private final BitSetIndex                    dpidCounter;
	private final BitSetIndex                    linkCounter;
	private final BitSetIndex                    ipCounter;
	private final BitSetIndex                    hostCounter;
	private final Map<OVXPort, Host>                    hostMap;
	private final OVXFlowManager		 flowManager;
	
	private NetworkState state;
	
	/**
	 * OVXNetwork constructor.
	 * Only use if you have reserved the tenantId beforehand!
	 * @param tenantId
	 * @param protocol
	 * @param controllerHost
	 * @param controllerPort
	 * @param network
	 * @param mask
	 * @throws IndexOutOfBoundException
	 */
	public OVXNetwork(final int tenantId, final ArrayList<String> controllerUrls, final IPAddress network,
			final short mask) throws IndexOutOfBoundException {
		super();
		this.tenantId = tenantId;
		this.controllerUrls = new HashSet<String>();
		this.controllerUrls.addAll(controllerUrls);
		this.network = network;
		this.mask = mask;
		this.dpidCounter = new BitSetIndex(IndexType.SWITCH_ID);
		this.linkCounter = new BitSetIndex(IndexType.LINK_ID);
		this.ipCounter = new BitSetIndex(IndexType.IP_ID);
		this.hostCounter = new BitSetIndex(IndexType.HOST_ID);
		this.hostMap = new HashMap<OVXPort, Host>();
		this.flowManager = new OVXFlowManager(this.tenantId, this.hostMap.values());
		this.state = NetworkState.INIT;
	}


	public OVXNetwork(final ArrayList<String> controllerUrls, final IPAddress network,
			final short mask) throws IndexOutOfBoundException {
		this(OpenVirteXController.getTenantCounter().getNewIndex(), controllerUrls, network, mask);
	}
	
	public Set<String> getControllerUrls() {
		return  Collections.unmodifiableSet(this.controllerUrls);
	}

	public Integer getTenantId() {
		return this.tenantId;
	}

	public IPAddress getNetwork() {
		return this.network;
	}

	public static void reserveTenantId(Integer tenantId) 
			throws IndexOutOfBoundException, DuplicateIndexException {
		OpenVirteXController.getTenantCounter().getNewIndex(tenantId);
	}

	public BitSetIndex getLinkCounter() {
		return this.linkCounter;
	}

	public BitSetIndex getHostCounter() {
		return this.hostCounter;
	}

	public MACAddress getGateway(final IPAddress ip) {
		return this.gwsMap.get(ip);
	}

	public short getMask() {
		return this.mask;
	}

	public OVXFlowManager getFlowManager() {
		return flowManager;
	}

	public void register() {
		this.state.register(this);
	}

	public boolean isBooted() {
		return this.state.equals(NetworkState.ACTIVE);
	}

	public Collection<Host> getHosts() {
		return Collections.unmodifiableCollection(this.hostMap.values());
	}

	/*public Host getHost(final MACAddress mac) {
		for (final Host host : this.hostList) {
			if (host.getMac().toLong() == mac.toLong()) {
				return host;
			}
		}
		return null;
	}*/

	public Host getHost(final OVXPort port) {
		return this.hostMap.get(port);
	}

	public Host getHost(final Integer hostId) {
		for (final Host host : this.hostMap.values()) {
			if (host.getHostId().equals(hostId)) {
				return host;
			}
		}
		return null;
	}

	public void unregister() {
		this.state.unregister(this);
	}

	// API-facing methods

	public OVXSwitch createSwitch(final List<Long> dpids, final long switchId)
			throws IndexOutOfBoundException {
		OVXSwitch virtualSwitch;
		/*
		 * The switchId is generated using the ON.Lab OUI (00:A4:23:05)
		 * plus a unique number inside the virtual network
		 */
		final List<PhysicalSwitch> switches = new ArrayList<PhysicalSwitch>();
		// TODO: check if dpids are present in physical network
		for (final long dpid : dpids) {
			switches.add(PhysicalNetwork.getInstance().getSwitch(dpid));
		}
		if (dpids.size() == 1) {
			virtualSwitch = new OVXSingleSwitch(switchId, this.tenantId);
		} else {
			virtualSwitch = new OVXBigSwitch(switchId, this.tenantId);
		}
		// Add switch to topology and register it in the map
		this.addSwitch(virtualSwitch);

		virtualSwitch.register(switches);
		if (this.isBooted())
			virtualSwitch.boot();

		return virtualSwitch;
	}


	public OVXSwitch createSwitch(final List<Long> dpids) throws IndexOutOfBoundException {
		final long switchId = (long) 0xa42305 << 32
				| this.dpidCounter.getNewIndex();
		return this.createSwitch(dpids, switchId);
	}

	public OVXPort createPort(final long physicalDpid, final short portNumber, final short... vportNumber) 
			throws IndexOutOfBoundException {
		final PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance()
				.getSwitch(physicalDpid);
		final PhysicalPort physicalPort = physicalSwitch.getPort(portNumber);

		final OVXPort ovxPort;
		if (vportNumber.length == 0)
			ovxPort = new OVXPort(this.tenantId, physicalPort, true);
		else
			ovxPort = new OVXPort(this.tenantId, physicalPort, true, vportNumber[0]);
		ovxPort.register();
		return ovxPort;
	}

	public RoutingAlgorithms setOVXBigSwitchRouting (final long dpid, final String alg, 
			final byte numBackups) throws RoutingAlgorithmException {
		RoutingAlgorithms algorithm = new RoutingAlgorithms(alg, numBackups);
		((OVXBigSwitch) this.getSwitch(dpid)).setAlg(algorithm);
		return algorithm;
	}

	public Host connectHost(final long ovxDpid, final short ovxPort,
			final MACAddress mac, final int hostId) throws IndexOutOfBoundException {
		OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
		port.boot();
		final Host host = new Host(mac, port, hostId);
		host.register();
		host.boot();
		return host;
	}

	public Host connectHost(final long ovxDpid, final short ovxPort,
			final MACAddress mac) throws IndexOutOfBoundException {
		return this.connectHost(ovxDpid, ovxPort, mac, this.hostCounter.getNewIndex());
	}

	/**
	 * Create link and add it to the topology. Returns linkId when successful,
	 * -1 if source port is already used.
	 * 
	 * @param srcPort
	 * @param dstPort
	 * @return
	 * @throws IndexOutOfBoundException
	 * @throws PortMappingException 
	 */
	public synchronized OVXLink connectLink(final long ovxSrcDpid, final short ovxSrcPort,
			final long ovxDstDpid, final short ovxDstPort, final String alg, final byte numBackups, final int linkId)
					throws IndexOutOfBoundException, PortMappingException {
		RoutingAlgorithms algorithm = null;
		try {
			algorithm = new RoutingAlgorithms(alg, numBackups);
		} catch (RoutingAlgorithmException e) {
			log.error("The algorithm provided ({}) is currently not supported."
					+ " Use default: shortest-path with one backup route.", alg);
			try {
				algorithm = new RoutingAlgorithms("spf", (byte)1);
			} catch (RoutingAlgorithmException e1) {}
		}

		//get the virtual end ports
		OVXPort srcPort = this.getSwitch(ovxSrcDpid).getPort(ovxSrcPort);
		srcPort.boot();
		OVXPort dstPort = this.getSwitch(ovxDstDpid).getPort(ovxDstPort);
		dstPort.boot();
		
		// Create link, add it to the topology, register it in the map(if algo=spf)
		// TODO separate link registration to clean this up.
		OVXLink link = new OVXLink(linkId, this.tenantId, srcPort,
				dstPort, algorithm);
		OVXLink reverseLink = new OVXLink(linkId, this.tenantId, dstPort,
				srcPort, algorithm);
		return link;
	}

	public synchronized OVXLink connectLink(final long ovxSrcDpid, final short ovxSrcPort,
			final long ovxDstDpid, final short ovxDstPort, final String alg, final byte numBackups ) 
					throws IndexOutOfBoundException, PortMappingException {
		final int linkId = this.linkCounter.getNewIndex();
		return this.connectLink(ovxSrcDpid, ovxSrcPort, ovxDstDpid, ovxDstPort, alg, numBackups, linkId);
	}    

	/**
	 * Create link and add it to the topology. Returns linkId when successful,
	 * -1 if source port is already used.
	 * 
	 * @param srcPort
	 * @param dstPort
	 * @return
	 * @throws IndexOutOfBoundException
	 */
	public synchronized OVXLink setLinkPath(final int linkId, final List<PhysicalLink> physicalLinks, 
			final byte priority)
					throws IndexOutOfBoundException {
		// create the map to the reverse list of physical links
		final List<PhysicalLink> reversePhysicalLinks = new LinkedList<PhysicalLink>();
		for (final PhysicalLink phyLink : Lists.reverse(physicalLinks)) {
			reversePhysicalLinks.add(PhysicalNetwork.getInstance().getLink(phyLink.getDstPort(),
					phyLink.getSrcPort()));
		}

		List<OVXLink> links = this.getLinksById(linkId);
		/*
		 * TODO: links is a list, so i need to check is the first link has to be mapped to the physicalPath or viceversa.
		 * If we'll split the link creation, don't need this check
		 */
		OVXLink link = null; 
		OVXLink reverseLink = null;
		if (links.get(0).getSrcPort().getPhysicalPort().equals(physicalLinks.get(0).getSrcPort())) {
			link = links.get(0);
			reverseLink = links.get(1);
		}
		else if (links.get(1).getSrcPort().getPhysicalPort().equals(physicalLinks.get(0).getSrcPort())) {
			link = links.get(1);
			reverseLink = links.get(0);
		}
		else log.error("Cannot retrieve the virtual links associated to linkId {}",linkId);
		link.register(physicalLinks, priority);
		reverseLink.register(reversePhysicalLinks, priority);
		link.boot();
		reverseLink.boot();
		return link;
	}

	public synchronized SwitchRoute connectRoute(final long ovxDpid, final short ovxSrcPort,
			final short ovxDstPort, final List<PhysicalLink> physicalLinks, 
			final byte priority, final int... routeId)
					throws IndexOutOfBoundException {
		OVXBigSwitch sw = (OVXBigSwitch) this.getSwitch(ovxDpid);
		OVXPort srcPort = sw.getPort(ovxSrcPort);
		OVXPort dstPort = sw.getPort(ovxDstPort);

		List<PhysicalLink> reverseLinks = new LinkedList<PhysicalLink>();
		for (PhysicalLink link : physicalLinks) {
			PhysicalLink revLink = new PhysicalLink(link.getDstPort(), link.getSrcPort());
			reverseLinks.add(revLink);
		}
		Collections.reverse(reverseLinks);
		SwitchRoute route;
		if (routeId.length == 0)
			route = sw.createRoute(srcPort, dstPort, physicalLinks, reverseLinks, priority);
		else
			route = sw.createRoute(srcPort, dstPort, physicalLinks, reverseLinks, priority, routeId[0]);

		route.register();

		return route;
	}

	public synchronized void removeSwitch(final long ovxDpid) {
		this.dpidCounter.releaseIndex((int) (0x000000 << 32 | ovxDpid));
		OVXSwitch sw = this.getSwitch(ovxDpid);
		sw.tearDown();
		sw.unregister();
	}

	public synchronized void removePort(final long ovxDpid, final short ovxPort) {
		OVXSwitch vsw = this.getSwitch(ovxDpid); 
		OVXPort port = vsw.getPort(ovxPort);
		if (port == null) {
			log.warn("port number {} not associated with any ports on switch {}",
					ovxPort, vsw.getSwitchName());
			return;
		}
		port.tearDown();
		port.unregister();
	}

	public synchronized void disconnectHost(final int hostId) {
		Host host = this.getHost(hostId);
		host.getPort().tearDown();
		host.unregister();
		this.hostCounter.releaseIndex(hostId);
	}

	public synchronized void disconnectLink(final int linkId) {
		LinkedList<OVXLink> linkPair = (LinkedList<OVXLink>) this.getLinksById(linkId);
		this.linkCounter.releaseIndex(linkPair.getFirst().getLinkId());
		for (OVXLink link : linkPair) {
			link.tearDown();
			link.unregister();
		}
	}

	public synchronized void disconnectRoute(final long ovxDpid, final int routeId) {
		OVXBigSwitch sw = (OVXBigSwitch) this.getSwitch(ovxDpid);
		sw.unregisterRoute(routeId);
	}

	public synchronized void startSwitch(final long ovxDpid) {
		OVXSwitch sw = this.getSwitch(ovxDpid);
		sw.boot();
	}


	public synchronized void startPort(final long ovxDpid, final short ovxPort) {
		OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
		/* Administratively enable port */
		port.setConfig(
				port.getConfig() & ~(OFPortConfig.OFPPC_PORT_DOWN.getValue()));
		port.boot();
	}

	public synchronized void stopSwitch(final long ovxDpid) {
		OVXSwitch sw = this.getSwitch(ovxDpid);
		sw.tearDown();
	}

	public synchronized void stopPort(final long ovxDpid, final short ovxPort) {
		OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
		/* Administratively disable port */
		port.setConfig(
				port.getConfig() | OFPortConfig.OFPPC_PORT_DOWN.getValue());
		port.tearDown();
	}

	/**
	 * Boots the virtual network by booting each virtual switch. TODO: we should
	 * roll-back if any switch fails to boot
	 * 
	 * @return True if successful, false otherwise
	 */
	@Override
	public boolean boot() {
		return this.state.boot(this);
	}


	/**
	 * Handle LLDP received from controller.
	 * 
	 * Receive LLDP from controller. Switch to which it is destined is passed in
	 * by the ControllerHandler, port is extracted from the packet_out.
	 * Packet_in is created based on topology info.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void handleLLDP(final OFMessage msg, final Switch sw) {
		this.state.handleLLDP(this, msg, sw);
	}

	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		// Do nothing
	}

	@Override
	public String getName() {
		return "Virtual network:" + this.tenantId.toString();
	}

	public Integer nextIP() throws IndexOutOfBoundException {
		return (this.tenantId << 32 - OpenVirteXController.getInstance()
				.getNumberVirtualNets()) + this.ipCounter.getNewIndex();
	}

	// TODO should this be here? OVXNetwork seems to have too narrow a view for this to be safe. 
	public static void reset() {
		OVXNetwork.log
		.debug("Resetting tenantId counter to initial state. Don't do this at runtime!");
		OpenVirteXController.getTenantCounter().reset();

	}

	public List<OVXLink> getLinksById(final Integer linkId) {
		final List<OVXLink> linkList = new LinkedList<OVXLink>();
		for (OVXLink link : this.getLinks()) {
			if (link.getLinkId().equals(linkId)) {
				linkList.add(link);
			}
		}
		return linkList;
	}

	//	@Override
	//	public Integer getDBIndex() {
	//		return this.tenantId;
	//	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.tenantId);
		return index;
	}

	@Override
	public String getDBKey() {
		return "vnet";
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.put(TenantHandler.TENANT, this.tenantId);
		dbObject.put(TenantHandler.CTRLURLS, this.controllerUrls);
		dbObject.put(TenantHandler.NETADD, this.network.getIp());
		dbObject.put(TenantHandler.NETMASK, this.mask);
		dbObject.put(TenantHandler.IS_BOOTED, this.isBooted());
		return dbObject;
	}


	public Set<OVXLink> getLinkSet() {
		return Collections.unmodifiableSet(this.linkSet);
	}

	@Override
	public boolean removeLink(final OVXLink virtualLink) {
		return super.removeLink(virtualLink);
	}
	
	@Override
	public void addLink(final OVXLink virtualLink) {
		super.addLink(virtualLink);
	}

	@Override
	public boolean removeSwitch(final OVXSwitch ovxSwitch) {
		return this.switchSet.remove(ovxSwitch);
	}
	
	/** Registers a host with this network. */
	public void addHost(final Host host) {
		this.hostMap.put(host.getPort(), host);
	}

	/** Unregisters a host from this network. */
	public void removeHost(final Host host) {
		this.hostMap.remove(host.getPort());
		this.hostCounter.releaseIndex(host.getHostId());
	}


	@Override
	public boolean tearDown() {
		return this.state.teardown(this);
	}


	public void addControllers(ArrayList<String> ctrlUrls) {
		this.controllerUrls.addAll(ctrlUrls);
		
	}
}
