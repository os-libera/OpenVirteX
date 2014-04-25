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
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import com.google.common.collect.Lists;

/**
 * Virtual networks contain tenantId, controller info, subnet and gateway
 * information. Handles registration of virtual switches and links. Responds to
 * LLDP discovery probes from the controller.
 *
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> implements
        Persistable {

    private static Logger log = LogManager
            .getLogger(OVXNetwork.class.getName());

    private final Integer tenantId;
    private final HashSet<String> controllerUrls;
    private final IPAddress network;
    private final short mask;
    private boolean isBooted;
    private final BitSetIndex dpidCounter;
    private final BitSetIndex linkCounter;
    private final BitSetIndex ipCounter;
    private final BitSetIndex hostCounter;
    private final Map<OVXPort, Host> hostMap;
    private final OVXFlowManager flowManager;

    /**
     * Instantiates a virtual network. Only use if you have reserved the tenantId
     * beforehand!
     *
     * @param tenantId the unique tenant ID
     * @param controllerUrls the list of controller URLs
     * @param network the virtual network address space
     * @param mask the virtual network address space mask
     * @throws IndexOutOfBoundException
     */
    public OVXNetwork(final int tenantId,
            final ArrayList<String> controllerUrls, final IPAddress network,
            final short mask) throws IndexOutOfBoundException {
        super();
        this.tenantId = tenantId;
        this.controllerUrls = new HashSet<String>();
        this.controllerUrls.addAll(controllerUrls);
        this.network = network;
        this.mask = mask;
        this.isBooted = false;
        this.dpidCounter = new BitSetIndex(IndexType.SWITCH_ID);
        this.linkCounter = new BitSetIndex(IndexType.LINK_ID);
        this.ipCounter = new BitSetIndex(IndexType.IP_ID);
        this.hostCounter = new BitSetIndex(IndexType.HOST_ID);
        this.hostMap = new HashMap<OVXPort, Host>();
        this.flowManager = new OVXFlowManager(this.tenantId,
                this.hostMap.values());
    }

    /**
     * Instantiates a virtual network, and assigns a unique tenant ID.
     *
     * @param controllerUrls the list of controller URLs
     * @param network the virtual network address space
     * @param mask the virtual network address space mask
     * @throws IndexOutOfBoundException
     */
    public OVXNetwork(final ArrayList<String> controllerUrls,
            final IPAddress network, final short mask)
            throws IndexOutOfBoundException {
        this(OpenVirteXController.getTenantCounter().getNewIndex(),
                controllerUrls, network, mask);
    }

    /**
     * Gets the list of controller URLs.
     *
     * @return the list of controller URLs
     */
    public Set<String> getControllerUrls() {
        return Collections.unmodifiableSet(this.controllerUrls);
    }

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    public Integer getTenantId() {
        return this.tenantId;
    }

    /**
     * Gets the network address space.
     *
     * @return the network address space
     */
    public IPAddress getNetwork() {
        return this.network;
    }

    /**
     * Reserves a unique tenant ID so it is guaranteed to be unique.
     *
     * @param tenantId the tenant ID
     * @throws IndexOutOfBoundException
     * @throws DuplicateIndexException
     */
    public static void reserveTenantId(Integer tenantId)
            throws IndexOutOfBoundException, DuplicateIndexException {
        OpenVirteXController.getTenantCounter().getNewIndex(tenantId);
    }

    /**
     * Gets the current value of the link ID.
     *
     * @return the current link ID
     */
    public BitSetIndex getLinkCounter() {
        return this.linkCounter;
    }

    /**
     * Gets the current value of the host ID.
     *
     * @return the current host ID
     */
    public BitSetIndex getHostCounter() {
        return this.hostCounter;
    }

    public short getMask() {
        return this.mask;
    }

    public OVXFlowManager getFlowManager() {
        return flowManager;
    }

    public void register() {
        OVXMap.getInstance().addNetwork(this);
        DBManager.getInstance().createDoc(this);
    }

    public boolean isBooted() {
        return this.isBooted;
    }

    public Collection<Host> getHosts() {
        return Collections.unmodifiableCollection(this.hostMap.values());
    }

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
        DBManager.getInstance().removeDoc(this);
        final LinkedList<Long> dpids = new LinkedList<>();
        for (final OVXSwitch virtualSwitch : this.getSwitches()) {
            dpids.add(virtualSwitch.getSwitchId());
        }
        for (final Long dpid : dpids) {
            this.getSwitch(dpid).unregister();
        }
        // remove the network from the Map
        OVXMap.getInstance().removeVirtualIPs(this.tenantId);
        OVXMap.getInstance().removeNetwork(this);
        OpenVirteXController.getTenantCounter().releaseIndex(this.tenantId);
    }

    public void stop() {
        for (final OVXSwitch sw : this.getSwitches()) {
            sw.tearDown();
        }
        this.isBooted = false;
    }

    // API-facing methods

    /**
     * Creates a virtual switch that is mapped to the given list of
     * physical switch DPIDs and sets the virtual switch DPID.
     *
     * @param dpids the list of physical switch DPIDs
     * @param switchId the virtual switch DPID
     * @return the virtual switch instance
     * @throws IndexOutOfBoundException
     */
    public OVXSwitch createSwitch(final List<Long> dpids, final long switchId)
            throws IndexOutOfBoundException {
        OVXSwitch virtualSwitch;
        /*
         * The switchId is generated using the ON.Lab OUI (00:A4:23:05) plus a
         * unique number inside the virtual network
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
        if (this.isBooted) {
            virtualSwitch.boot();
        }

        return virtualSwitch;
    }

    /**
     * Creates a virtual switch that is mapped to the given list
     * of physical switch DPIDs.
     *
     * @param dpids the list of physical switch DPID
     * @return the virtual switch instance
     * @throws IndexOutOfBoundException
     */
    public OVXSwitch createSwitch(final List<Long> dpids)
            throws IndexOutOfBoundException {
        final long switchId = (long) 0xa42305 << 32
                | this.dpidCounter.getNewIndex();
        return this.createSwitch(dpids, switchId);
    }

    /**
     * Creates a virtual port that is mapped to the given physical
     * switch DPID and port number, and set its virtual port number
     * if present.
     *
     * @param physicalDpid the physical DPID
     * @param portNumber the physical port number
     * @param vportNumber the virtual port number
     * @return the virtual port instance
     * @throws IndexOutOfBoundException
     */
    public OVXPort createPort(final long physicalDpid, final short portNumber,
            final short... vportNumber) throws IndexOutOfBoundException {
        final PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance()
                .getSwitch(physicalDpid);
        final PhysicalPort physicalPort = physicalSwitch.getPort(portNumber);

        final OVXPort ovxPort;
        if (vportNumber.length == 0) {
            ovxPort = new OVXPort(this.tenantId, physicalPort, true);
        } else {
            ovxPort = new OVXPort(this.tenantId, physicalPort, true,
                    vportNumber[0]);
        }
        ovxPort.register();
        return ovxPort;
    }

    /**
     * Sets the algorithm and number of backups for the
     * big switch routing.
     *
     * @param dpid the virtual dpid
     * @param alg the algorithm
     * @param numBackups the number of backups
     * @return the routing algorithm instance
     * @throws RoutingAlgorithmException
     */
    public RoutingAlgorithms setOVXBigSwitchRouting(final long dpid,
            final String alg, final byte numBackups)
            throws RoutingAlgorithmException {
        RoutingAlgorithms algorithm = new RoutingAlgorithms(alg, numBackups);
        ((OVXBigSwitch) this.getSwitch(dpid)).setAlg(algorithm);
        return algorithm;
    }

    /**
     * Connects the host identified by unique MAC address and
     * unique host ID to the given virtual switch port,
     * and returns the host instance.
     *
     * @param ovxDpid the virtual switch ID
     * @param ovxPort the virtual port number
     * @param mac the MAC address
     * @param hostId the host ID
     * @return the host instance
     * @throws IndexOutOfBoundException
     */
    public Host connectHost(final long ovxDpid, final short ovxPort,
            final MACAddress mac, final int hostId)
            throws IndexOutOfBoundException {
        OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
        port.boot();
        OVXMap.getInstance().addMAC(mac, this.tenantId);
        final Host host = new Host(mac, port, hostId);
        this.hostMap.put(port, host);
        host.register();
        return host;
    }

    /**
     * Connects the host identified by unique MAC address
     * host ID to the given virtual switch port,
     * and returns the host instance. This method will generate
     * a unique host ID.
     *
     * @param ovxDpid the virtual switch dpdi
     * @param ovxPort the virtual switch port number
     * @param mac the MAC address
     * @return the host instance
     * @throws IndexOutOfBoundException
     */
    public Host connectHost(final long ovxDpid, final short ovxPort,
            final MACAddress mac) throws IndexOutOfBoundException {
        return this.connectHost(ovxDpid, ovxPort, mac,
                this.hostCounter.getNewIndex());
    }

    /**
     * Creates virtual link, adds it to the topology, and returns the link instance.
     *
     * @param ovxSrcDpid virtual source dpid
     * @param ovxSrcPort source port number
     * @param ovxDstDpid virtual destination dpid
     * @param ovxDstPort destination port number
     * @param alg the routing algorithm
     * @param numBackups the number of backups
     * @param linkId the link ID
     * @return the virtual link instance
     * @throws IndexOutOfBoundException
     * @throws PortMappingException
     */
    public synchronized OVXLink connectLink(final long ovxSrcDpid,
            final short ovxSrcPort, final long ovxDstDpid,
            final short ovxDstPort, final String alg, final byte numBackups,
            final int linkId) throws IndexOutOfBoundException,
            PortMappingException {
        RoutingAlgorithms algorithm = null;
        try {
            algorithm = new RoutingAlgorithms(alg, numBackups);
        } catch (RoutingAlgorithmException e) {
            log.error("The algorithm provided ({}) is currently not supported."
                    + " Use default: shortest-path with one backup route.", alg);
            try {
                algorithm = new RoutingAlgorithms("spf", (byte) 1);
            } catch (RoutingAlgorithmException e1) {
                log.error("Could not connect link: {}", e1);
                return null;
            }
        }

        // get the virtual end ports
        OVXPort srcPort = this.getSwitch(ovxSrcDpid).getPort(ovxSrcPort);
        OVXPort dstPort = this.getSwitch(ovxDstDpid).getPort(ovxDstPort);

        // Create link, add it to the topology, register it in the map
        OVXLink link = new OVXLink(linkId, this.tenantId, srcPort, dstPort,
                algorithm);
        OVXLink reverseLink = new OVXLink(linkId, this.tenantId, dstPort,
                srcPort, algorithm);
        super.addLink(link);
        super.addLink(reverseLink);
        log.info(
                "Created bi-directional virtual link {} between ports {}/{} - {}/{} in virtual network {}",
                link.getLinkId(), link.getSrcSwitch().getSwitchName(),
                srcPort.getPortNumber(), link.getDstSwitch().getSwitchName(),
                dstPort.getPortNumber(), this.getTenantId());
        srcPort.setEdge(false);
        dstPort.setEdge(false);
        srcPort.boot();
        dstPort.boot();
        return link;
    }

    /**
     * Creates virtual link between given virtual source port
     * and virtual destination port, creates a unique link ID,
     * creates its mapping to the physical
     * topology, and adds it to the topology.
     *
     * @param ovxSrcDpid the virtual source DPID
     * @param ovxSrcPort the virtual source port
     * @param ovxDstDpid the virtual destination DPID
     * @param ovxDstPort the virtual destination DPID
     * @param alg the algorithm
     * @param numBackups the number of backups
     * @return the virtual link instance
     * @throws IndexOutOfBoundException
     * @throws PortMappingException
     */
    public synchronized OVXLink connectLink(final long ovxSrcDpid,
            final short ovxSrcPort, final long ovxDstDpid,
            final short ovxDstPort, final String alg, final byte numBackups)
            throws IndexOutOfBoundException, PortMappingException {
        final int linkId = this.linkCounter.getNewIndex();
        return this.connectLink(ovxSrcDpid, ovxSrcPort, ovxDstDpid, ovxDstPort,
                alg, numBackups, linkId);
    }

    /**
     * Creates virtual link mapping to the physical topology.
     *
     * @param linkId the unique link ID
     * @param physicalLinks the list of physical links
     * @param priority the priority value
     * @return the virtual link instance
     * @throws IndexOutOfBoundException
     */
    public synchronized OVXLink setLinkPath(final int linkId,
            final List<PhysicalLink> physicalLinks, final byte priority)
            throws IndexOutOfBoundException {
        // create the map to the reverse list of physical links
        final List<PhysicalLink> reversePhysicalLinks = new LinkedList<PhysicalLink>();
        for (final PhysicalLink phyLink : Lists.reverse(physicalLinks)) {
            reversePhysicalLinks.add(PhysicalNetwork.getInstance().getLink(
                    phyLink.getDstPort(), phyLink.getSrcPort()));
        }

        List<OVXLink> links = this.getLinksById(linkId);
        /*
         * TODO: links is a list, so i need to check is the first link has to be
         * mapped to the physicalPath or viceversa. If we'll split the link
         * creation, don't need this check
         */
        OVXLink link = null;
        OVXLink reverseLink = null;
        if (links.get(0).getSrcPort().getPhysicalPort() == physicalLinks.get(0)
                .getSrcPort()) {
            link = links.get(0);
            reverseLink = links.get(1);
        } else if (links.get(1).getSrcPort().getPhysicalPort() == physicalLinks
                .get(0).getSrcPort()) {
            link = links.get(1);
            reverseLink = links.get(0);
        } else {
            log.error(
                    "Cannot retrieve the virtual links associated to linkId {}",
                    linkId);
        }
        link.register(physicalLinks, priority);
        reverseLink.register(reversePhysicalLinks, priority);
        return link;
    }

    public synchronized SwitchRoute connectRoute(final long ovxDpid,
            final short ovxSrcPort, final short ovxDstPort,
            final List<PhysicalLink> physicalLinks, final byte priority,
            final int... routeId) throws IndexOutOfBoundException {
        OVXBigSwitch sw = (OVXBigSwitch) this.getSwitch(ovxDpid);
        OVXPort srcPort = sw.getPort(ovxSrcPort);
        OVXPort dstPort = sw.getPort(ovxDstPort);

        List<PhysicalLink> reverseLinks = new LinkedList<PhysicalLink>();
        for (PhysicalLink link : physicalLinks) {
            PhysicalLink revLink = new PhysicalLink(link.getDstPort(),
                    link.getSrcPort());
            reverseLinks.add(revLink);
        }
        Collections.reverse(reverseLinks);
        SwitchRoute route;
        if (routeId.length == 0) {
            route = sw.createRoute(srcPort, dstPort, physicalLinks,
                    reverseLinks, priority);
        } else {
            route = sw.createRoute(srcPort, dstPort, physicalLinks,
                    reverseLinks, priority, routeId[0]);
        }

        route.register();

        return route;
    }

    public synchronized void removeSwitch(final long ovxDpid) {
        this.dpidCounter.releaseIndex((int) (0x000000 << 32 | ovxDpid));
        OVXSwitch sw = this.getSwitch(ovxDpid);
        sw.unregister();
    }

    public synchronized void removePort(final long ovxDpid, final short ovxPort) {
        OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
        port.unregister();
    }

    public synchronized void disconnectHost(final int hostId) {
        Host host = this.getHost(hostId);
        host.unregister();
        this.hostCounter.releaseIndex(hostId);
        this.removeHost(host);
    }

    public synchronized void disconnectLink(final int linkId) {
        LinkedList<OVXLink> linkPair = (LinkedList<OVXLink>) this
                .getLinksById(linkId);
        this.linkCounter.releaseIndex(linkPair.getFirst().getLinkId());
        for (OVXLink link : linkPair) {
            link.unregister();
            this.removeLink(link);
        }
    }

    public synchronized void disconnectRoute(final long ovxDpid,
            final int routeId) {
        OVXBigSwitch sw = (OVXBigSwitch) this.getSwitch(ovxDpid);
        sw.unregisterRoute(routeId);
    }

    public synchronized void startSwitch(final long ovxDpid) {
        OVXSwitch sw = this.getSwitch(ovxDpid);
        sw.boot();
    }

    public synchronized void startPort(final long ovxDpid, final short ovxPort) {
        OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
        port.boot();
    }

    public synchronized void stopSwitch(final long ovxDpid) {
        OVXSwitch sw = this.getSwitch(ovxDpid);
        sw.tearDown();
    }

    public synchronized void stopPort(final long ovxDpid, final short ovxPort) {
        OVXPort port = this.getSwitch(ovxDpid).getPort(ovxPort);
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
        boolean result = true;
        try {
            flowManager.boot();
        } catch (final IndexOutOfBoundException e) {
            OVXNetwork.log
                    .error("Too many host to generate the flow pairs. Tear down the virtual network {}",
                            this.tenantId);
            this.stop();
            return false;
        }
        for (final OVXSwitch sw : this.getSwitches()) {
            result &= sw.boot();
        }
        this.isBooted = result;
        return this.isBooted;
    }

    /**
     * Handles LLDP received from controller.
     * Receive LLDP from controller. Switch to which it is destined is passed in
     * by the ControllerHandler, port is extracted from the packet_out.
     * Packet_in is created based on topology info.
     *
     * @param msg the OpenFlow message
     * @param sw the switch
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
        final OVXPacketOut po = (OVXPacketOut) msg;
        final byte[] pkt = po.getPacketData();

        // Create LLDP response for each output action port
        for (final OFAction action : po.getActions()) {
            try {
                final short portNumber = ((OFActionOutput) action).getPort();
                final OVXPort srcPort = (OVXPort) sw.getPort(portNumber);
                final OVXPort dstPort = this.getNeighborPort(srcPort);
                if (dstPort != null) {
                    final OVXPacketIn pi = new OVXPacketIn();
                    pi.setBufferId(OFPacketOut.BUFFER_ID_NONE);
                    // Get input port from pkt_out
                    pi.setInPort(dstPort.getPortNumber());
                    pi.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
                    pi.setPacketData(pkt);
                    pi.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + pkt.length));
                    dstPort.getParentSwitch().sendMsg(pi, this);
                }
            } catch (final ClassCastException c) {
                // ignore non-ActionOutput pkt_out's
                continue;
            }
        }

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

    // @Override
    // public Integer getDBIndex() {
    // return this.tenantId;
    // }

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
        dbObject.put(TenantHandler.IS_BOOTED, this.isBooted);
        return dbObject;
    }

    public Set<OVXLink> getLinkSet() {
        return Collections.unmodifiableSet(this.linkSet);
    }

    @Override
    public boolean removeLink(final OVXLink virtualLink) {
        return this.linkSet.remove(virtualLink);
    }

    @Override
    public boolean removeSwitch(final OVXSwitch ovxSwitch) {
        return this.switchSet.remove(ovxSwitch);
    }

    public void removeHost(final Host host) {
        this.hostMap.remove(host.getPort());
    }

    public void addControllers(ArrayList<String> ctrlUrls) {
        this.controllerUrls.addAll(ctrlUrls);

    }
}
