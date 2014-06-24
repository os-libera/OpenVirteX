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
package net.onrc.openvirtex.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.Resilient;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
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
import org.openflow.util.U8;

/**
 * This class presents an abstraction for a route within a big switch. A route
 * consists of a virtual switch, the ingress and egress ports on that switch, a
 * priority value, and it has a unique route ID. It stores the mapping to one or
 * more physical paths, each of which have a priority. Several methods are
 * available to create physical paths It also exposes the methods needed to
 * generate and install the flow mods on the physical plane, based on a virtual
 * (controller-generated) flow mod.
 *
 */
public class SwitchRoute extends Link<OVXPort, PhysicalSwitch> implements
Persistable, Resilient {

    /**
     * The FSM for a SwitchRoute.
     */
    enum RouteState {
        INIT {
            @Override
            protected void register(final SwitchRoute route,
                    final List<PhysicalLink> path) {
                SwitchRoute.log.debug("registering route {}-{}",
                        route.srcPort.toAP(), route.dstPort.toAP());
                route.state = RouteState.INACTIVE;

                final OVXBigSwitch vsw = (OVXBigSwitch) route.sw;
                /*
                 * SwitchRoutes don't necessarily have PhysicalLinks mapped to
                 * it.
                 */
                if (path != null && !path.isEmpty()) {
                    vsw.getMap().addRoute(route, path);
                }
                vsw.registerRoute(route.srcPort, route.dstPort, route);
                DBManager.getInstance().save(route);
            }
        },
        INACTIVE {
            @Override
            protected boolean boot(final SwitchRoute route) {
                SwitchRoute.log.debug("enabling route {}-{}",
                        route.srcPort.toAP(), route.dstPort.toAP());
                route.state = RouteState.ACTIVE;
                return true;
            }

            @Override
            protected void unregister(final SwitchRoute route) {
                SwitchRoute.log.debug("unregistering route {}-{}",
                        route.srcPort.toAP(), route.dstPort.toAP());
                route.state = RouteState.STOPPED;

                final OVXBigSwitch vsw = (OVXBigSwitch) route.sw;
                vsw.getMap().removeRoute(route);
                vsw.unregisterRoute(route.srcPort, route.dstPort, route.routeId);
                DBManager.getInstance().remove(route);
            }

            @Override
            public boolean tryRevert(final SwitchRoute route,
					final PhysicalLink plink) {
                if (route.unusableRoutes.isEmpty()) {
                    return false;
                }
                synchronized (route.unusableRoutes) {
                    final Iterator<Byte> it = route.unusableRoutes
							.descendingKeySet().iterator();
                    while (it.hasNext()) {
                        final Byte curPriority = it.next();
                        if (route.unusableRoutes.get(curPriority).contains(
                                plink)) {
                            SwitchRoute.log
                            .info("Reactivate all inactive paths for virtual network {} big-switch {} "
                                    + "internal route {} between ports ({},{})",
                                    route.getTenantId(), route
                                    .getSrcPort()
                                    .getParentSwitch()
                                    .getSwitchName(),
                                    route.routeId, route.getSrcPort()
                                    .getPortNumber(), route
                                    .getDstPort()
                                    .getPortNumber());
                            if (U8.f(route.getPriority()) >= U8.f(curPriority)) {
                                route.backupRoutes.put(curPriority,
                                        route.unusableRoutes.get(curPriority));
                            }
                            try {
                                final List<PhysicalLink> backupLinks = new ArrayList<>(
                                        OVXMap.getInstance().getRoute(route));
                                Collections.copy(backupLinks, OVXMap
                                        .getInstance().getRoute(route));
                                route.backupRoutes.put(route.getPriority(),
                                        backupLinks);
                                route.switchPath(
                                        route.unusableRoutes.get(curPriority),
                                        curPriority);
                            } catch (final LinkMappingException e) {
                                SwitchRoute.log
                                .warn("No physical Links mapped to SwitchRoute? : {}",
                                        e);
                                return false;
                            }
                            it.remove();
                        }
                    }
                }
                route.backupRoutes.remove(route.priority);
                route.boot();
                return true;
            }
        },
        ACTIVE {
            @Override
            protected boolean teardown(final SwitchRoute route) {
                SwitchRoute.log.debug("disabling route {}-{}",
                        route.srcPort.toAP(), route.dstPort.toAP());
                route.state = RouteState.INACTIVE;
                return true;
            }

            @Override
            public boolean tryRecovery(final SwitchRoute route,
					final PhysicalLink plink) {
                SwitchRoute.log.debug(
                        "Try recovery for virtual network {} big-switch {} "
                                + "internal route {} between ports ({},{})",
                                route.getTenantId(), route.getSrcPort()
                                .getParentSwitch().getSwitchName(),
                                route.routeId, route.getSrcPort().getPortNumber(),
                                route.getDstPort().getPortNumber());
                /*
                 * store broken route to force route re-initialization when we
                 * tryRevert().
                 */
                try {
                    final List<PhysicalLink> unusableLinks = new ArrayList<>(
							OVXMap.getInstance().getRoute(route));
                    Collections.copy(unusableLinks, OVXMap.getInstance()
                            .getRoute(route));
                    route.unusableRoutes
                    .put(route.getPriority(), unusableLinks);
                } catch (final LinkMappingException e) {
                    SwitchRoute.log.warn(
                            "No physical Links mapped to SwitchRoute? : {}", e);
                    return false;
                }
                if (route.backupRoutes.size() > 0) {
                    List<PhysicalLink> phyLinks = null;
                    for (final Byte priority : route.backupRoutes
							.descendingKeySet()) {
                        // take highest priority backup avail.
                        phyLinks = route.backupRoutes.get(priority);
                        if (phyLinks.contains(plink)) {
                            SwitchRoute.log
                            .warn("backup candidate contains failed PhyLink {}-{}",
                                    plink.getSrcPort().toAP(), plink
                                    .getDstPort().toAP());
                        } else {
                            // switch route to new one in OVXMap, and new
                            // priority
                            route.switchPath(phyLinks, priority);
                            // remove selected backup from backups list.
                            route.backupRoutes.remove(priority);
                            SwitchRoute.log.debug(
                                    "Route{}:\n o backuproutes[{} routes]: \n\t{}"
                                            + "\n o unusable: \n\t{}",
                                            route.routeId, route.backupRoutes.size(),
                                            route.backupRoutes, route.unusableRoutes);
                            return true;
                        }
                    }
                }
                return false;
            }
        },
        STOPPED {

            @Override
            protected void replaceRoute(final SwitchRoute route,
					final Byte priority, final List<PhysicalLink> physicalLinks) {
            }

            @Override
            protected void addBackupRoute(final SwitchRoute route,
					final Byte prio, final List<PhysicalLink> phyLinks) {
            }

            @Override
            public void generateFMs(final SwitchRoute route, final OVXFlowMod fm) {
            }

        };

        protected void register(final SwitchRoute route,
				final List<PhysicalLink> path) {
            SwitchRoute.log.debug(
                    "Cannot register route {}-{} while status={}",
                    route.srcPort.toAP(), route.dstPort.toAP(), route.state);
        }

        protected boolean boot(final SwitchRoute route) {
            SwitchRoute.log.debug("Cannot boot route {}-{} while status={}",
                    route.srcPort.toAP(), route.dstPort.toAP(), route.state);
            return false;
        }

        protected boolean teardown(final SwitchRoute route) {
            SwitchRoute.log.debug(
                    "Cannot teardown route {}-{} while status={}",
                    route.srcPort.toAP(), route.dstPort.toAP(), route.state);
            return false;
        }

        protected void unregister(final SwitchRoute route) {
            SwitchRoute.log.debug(
                    "Cannot unregister route {}-{} while status={}",
                    route.srcPort.toAP(), route.dstPort.toAP(), route.state);
        }

        protected void addBackupRoute(final SwitchRoute route, final Byte prio,
                final List<PhysicalLink> phyLinks) {
            SwitchRoute.log.debug("Added backup for {}-{}, prio={}",
                    route.srcPort.toAP(), route.dstPort.toAP(), prio);
            route.backupRoutes.put(prio, phyLinks);
        }

        /* Called as long as route is ACTIVE or INACTIVE */
        protected void replaceRoute(final SwitchRoute route,
				final Byte priority, final List<PhysicalLink> physicalLinks) {
            // Save the current path in the backup Map
            try {
                route.addBackupRoute(route.priority, OVXMap.getInstance()
                        .getRoute(route));
            } catch (final LinkMappingException e) {
                SwitchRoute.log.error(
                        "Unable to retrieve the list of physical link from the OVXMap "
                                + "associated to the big-switch route {}",
                                route.routeId);
            }
            route.switchPath(physicalLinks, priority);
        }

        public boolean tryRecovery(final SwitchRoute route,
				final PhysicalLink plink) {
            SwitchRoute.log.debug(
                    "{}-{} Not active - call to tryRecovery failed.", route
                    .getSrcPort().toAP(), route.getDstPort().toAP());
            return false;
        }

        public boolean tryRevert(final SwitchRoute route,
				final PhysicalLink plink) {
            SwitchRoute.log.debug(
                    "{}-{} Not active - call to tryRevert failed.", route
                    .getSrcPort().toAP(), route.getDstPort().toAP());
            return false;
        }

        public void generateFMs(final SwitchRoute route, final OVXFlowMod fm) {
            route.generateFlowMods(fm);
        }

    }

    private static Logger log = LogManager.getLogger(SwitchRoute.class
            .getName());
    /**
     * Database keyword for switch routes.
     */
    public static final String DB_KEY = "routes";
    // Unique (for this switch) route identifier
    private int routeId;
    // Parent virtual switch
    private final OVXSwitch sw;
    private byte priority;
    private final TreeMap<Byte, List<PhysicalLink>> backupRoutes;
    private final TreeMap<Byte, List<PhysicalLink>> unusableRoutes;
    // A reference to the PhysicalPort at the start of the path
    private PhysicalPort inPort;
    // A reference to the PhysicalPort at the start of the path
    private PhysicalPort outPort;
    // The state of the route
    private RouteState state;

    /**
     * Instantiates a new switch route for the given switch between two ports,
     * and assigns a route ID and priority value.
     *
     * @param sw
     *            the virtual switch
     * @param in
     *            the ingress port
     * @param out
     *            the egress port
     * @param routeid
     *            the route ID
     * @param priority
     *            the priority value
     */
    public SwitchRoute(final OVXSwitch sw, final OVXPort in, final OVXPort out,
            final int routeid, final byte priority) {
        super(in, out);
        this.sw = sw;
        this.routeId = routeid;
        this.priority = priority;
        this.backupRoutes = new TreeMap<>();
        this.unusableRoutes = new TreeMap<>();
        this.state = RouteState.INIT;
    }

    /**
     * Sets the switch-unique identifier of this route.
     *
     * @param routeid
     *            the route ID
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
     * Gets the priority value.
     *
     * @return the priority value
     */
    public byte getPriority() {
        return this.priority;
    }

    /**
     * Sets the priority value of the switch route.
     *
     * @param priority
     *            the priority value
     */
    public void setPriority(final byte priority) {
        this.priority = priority;
    }

    /**
     * @return The DPID of the virtual switch
     */
    public long getSwitchId() {
        return this.sw.getSwitchId();
    }

    /**
     * Returns unique the tenant ID of the switch route.
     *
     * @return the tenant ID
     */
    public Integer getTenantId() {
        return this.sw.getTenantId();
    }

    /**
     * Sets the ingress port of the switch route to the given physical port.
     *
     * @param start
     *            the source physical port
     */
    public void setPathSrcPort(final PhysicalPort start) {
        this.inPort = start;
    }

    /**
     * @return the PhysicalPort at start of the route.
     */
    public PhysicalPort getPathSrcPort() {
        return this.inPort;
    }

    /**
     * Sets the egress port of the switch route to the given physical port.
     *
     * @param end
     *            the destination physical port
     */
    public void setPathDstPort(final PhysicalPort end) {
        this.outPort = end;
    }

    /**
     * @return the PhysicalPort at end of the route.
     */
    public PhysicalPort getPathDstPort() {
        return this.outPort;
    }

    @Override
    public String toString() {
        return "routeId: " + this.routeId + " dpid: " + this.getSwitchId()
                + " inPort: " + this.srcPort == null ? "" : this.srcPort
                        .toString() + " outPort: " + this.dstPort == null ? ""
                                : this.dstPort.toString();
    }

    /**
     * Adds a backup route with given priority value and path given as list of
     * physical links.
     *
     * @param priority
     *            the priority value
     * @param physicalLinks
     *            the backup route
     */
    public void addBackupRoute(final Byte priority,
            final List<PhysicalLink> physicalLinks) {
        this.state.addBackupRoute(this, priority, physicalLinks);
    }

    /**
     * Replaces the current primary route by the new route given as a list of
     * physical links and priority value.
     *
     * @param priority
     *            the priority value
     * @param physicalLinks
     *            the new route
     */
    public void replacePrimaryRoute(final Byte priority,
            final List<PhysicalLink> physicalLinks) {
        this.state.replaceRoute(this, priority, physicalLinks);
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

    /**
     * Switches over to the next backup path given as a list of physical links
     * with the given priority.
     *
     * @param physicalLinks
     *            the new path
     * @param priority
     *            the priority of the new path
     */
    public void switchPath(final List<PhysicalLink> physicalLinks,
			final byte priority) {
        // Register the new path as primary path in the OVXMap
        OVXMap.getInstance().removeRoute(this);
        OVXMap.getInstance().addRoute(this, physicalLinks);
        // Set the route priority to the new one
        this.setPriority(priority);
        int counter = 0;
        SwitchRoute.log
        .debug("Virtual network {}: switching all existing flow-mods crossing"
                + "the big-switch {} route {} between ports ({},{}) to the new path: {}",
                this.getTenantId(), this.getSrcPort().getParentSwitch()
                .getSwitchName(), this.getRouteId(), this
                .getSrcPort().getPortNumber(), this
                .getDstPort().getPortNumber(), physicalLinks);
        final Collection<OVXFlowMod> flows = this.getSrcPort()
				.getParentSwitch().getFlowTable().getFlowTable();
        for (final OVXFlowMod fe : flows) {
            for (final OFAction act : fe.getActions()) {
                if (act.getType() == OFActionType.OUTPUT
                        && fe.getMatch().getInputPort() == this.getSrcPort()
                        .getPortNumber()
                        && ((OFActionOutput) act).getPort() == this
                        .getDstPort().getPortNumber()) {
                    SwitchRoute.log
                    .info("Virtual network {}, switch {}, route {} between ports {}-{}: switch fm {}",
                            this.getTenantId(), this.getSrcPort()
                            .getParentSwitch().getSwitchName(),
                            this.getRouteId(), this.getSrcPort()
                            .getPortNumber(), this.getDstPort()
                            .getPortNumber(), fe);
                    counter++;

                    final OVXFlowMod fm = fe.clone();
                    fm.setCookie(((OVXFlowTable) this.getSrcPort()
                            .getParentSwitch().getFlowTable()).getCookie(fe,
                                    true));
                    this.generateRouteFMs(fm);
                    this.generateFirstFM(fm);
                }
            }
        }
        SwitchRoute.log
				.info("Virtual network {}, switch {}, route {} between ports {}-{}: {} flow-mod switched to the new path",
						this.getTenantId(), this.getSrcPort().getParentSwitch()
								.getSwitchName(), this.getRouteId(), this
								.getSrcPort().getPortNumber(), this
								.getDstPort().getPortNumber(), counter);
    }

    /**
     * Generates and installs all flow mods needed to bring up switch route,
     * base an a given controller-generated flow mod.
     *
     * @param fm
     *            the virtual flow mod
     */
    public void generateRouteFMs(final OVXFlowMod fm) {
        this.state.generateFMs(this, fm);
    }

    private void generateFlowMods(final OVXFlowMod fm) {
        SwitchRoute.log.info("generateFlowMods() for SR-{}-{}", this
				.getSrcPort().toAP(), this.getDstPort().toAP());
        // This list includes all the actions that have to be applied at the end
        // of the route
        final LinkedList<OFAction> outActions = new LinkedList<OFAction>();
        /*
         * Check the outPort: - if it's an edge, configure the route's last FM
         * to rewrite the IPs and generate the route FMs - if it's a link: -
         * retrieve the link - generate the link FMs - configure the route's
         * last FM to rewrite the MACs - generate the route FMs
         */
        if (this.getDstPort().isEdge()) {
            outActions.addAll(IPMapper.prependUnRewriteActions(fm.getMatch()));
        } else {
            final OVXLink link = this.getDstPort().getLink().getOutLink();
            final Integer linkId = link.getLinkId();
            Integer flowId = 0;
            try {
                flowId = OVXMap
                        .getInstance()
                        .getVirtualNetwork(this.getTenantId())
                        .getFlowManager()
                        .storeFlowValues(fm.getMatch().getDataLayerSource(),
                                fm.getMatch().getDataLayerDestination());
                link.generateLinkFMs(fm.clone(), flowId);
                outActions.addAll(new OVXLinkUtils(this.getTenantId(), linkId,
                        flowId).setLinkFields());
            } catch (final IndexOutOfBoundException e) {
                SwitchRoute.log.error(
                        "Too many host to generate the flow pairs in this virtual network {}. "
                                + "Dropping flow-mod {} ", this.getTenantId(),
                                fm);
            } catch (final NetworkMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /*
         * If the packet has L3 fields (e.g. NOT ARP), change the packet match:
         * 1) change the fields where the physical ips are stored
         */
        if (fm.getMatch().getDataLayerType() == Ethernet.TYPE_IPV4) {
            IPMapper.rewriteMatch(this.getSrcPort().getTenantId(),
                    fm.getMatch());
        }

        /*
         * Get the list of physical links mapped to this virtual link, in
         * REVERSE ORDER
         */
        PhysicalPort inPort = null;
        PhysicalPort outPort = null;
        fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);

        final SwitchRoute route = ((OVXBigSwitch) this.getSrcPort()
                .getParentSwitch()).getRoute(this.getSrcPort(),
                        this.getDstPort());
        final LinkedList<PhysicalLink> reverseLinks = new LinkedList<>();
        try {
            for (final PhysicalLink phyLink : OVXMap.getInstance().getRoute(
                    route)) {
                final PhysicalLink nlink = new PhysicalLink(
						phyLink.getDstPort(), phyLink.getSrcPort());
                reverseLinks.add(nlink);
                nlink.boot();
            }
        } catch (final LinkMappingException e) {
            SwitchRoute.log.warn("Could not fetch route : {}", e);
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
                phyLink.getSrcPort().getParentSwitch()
                .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
                SwitchRoute.log
                .debug("Sending big-switch route intermediate fm to sw {}: {}",
                        phyLink.getSrcPort().getParentSwitch()
                        .getName(), fm);

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
                phyLink.getSrcPort().getParentSwitch()
                .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
                SwitchRoute.log.debug(
                        "Sending big-switch route last fm to sw {}: {}",
                        phyLink.getSrcPort().getParentSwitch().getName(), fm);
            }
            outPort = phyLink.getDstPort();
        }

        // TODO: With POX we need to put a timeout between this flows and the
        // first flowMod. Check how to solve
        try {
            Thread.sleep(5);
        } catch (final InterruptedException e1) {
            SwitchRoute.log
            .warn("Timeout failed, might be a problem for POX controller: {}",
                    e1);
        }
    }

    /**
     * Generates and installs flow mod on the first physical switch of a switch
     * route, based an a controller-generated flow mod.
     *
     * @param fm
     *            the virtual flow mod
     */
    private void generateFirstFM(final OVXFlowMod fm) {
        fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        final List<OFAction> approvedActions = new LinkedList<OFAction>();
        if (this.getSrcPort().isLink()) {
            OVXPort dstPort = null;
            try {
                dstPort = OVXMap.getInstance()
                        .getVirtualNetwork(this.getTenantId())
                        .getNeighborPort(this.getSrcPort());
            } catch (final NetworkMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            final OVXLink link = this.getSrcPort().getLink().getOutLink();
            Integer flowId = 0;
            if (link != null) {
                try {
                    flowId = OVXMap
                            .getInstance()
                            .getVirtualNetwork(this.getTenantId())
                            .getFlowManager()
                            .getFlowId(fm.getMatch().getDataLayerSource(),
                                    fm.getMatch().getDataLayerDestination());
                } catch (final NetworkMappingException e) {
                    SwitchRoute.log
                    .warn("Error retrieving the network with id {} for flowMod {}. Dropping packet...",
                            this.getTenantId(), fm);
                    return;
                } catch (final DroppedMessageException e) {
                    SwitchRoute.log.warn(
                            "Error retrieving flowId in network with id {} for flowMod {}."
                                    + "Dropping packet...", this.getTenantId(),
                                    fm);
                    return;
                }
                final OVXLinkUtils lUtils = new OVXLinkUtils(
						this.getTenantId(), link.getLinkId(), flowId);
                lUtils.rewriteMatch(fm.getMatch());
                IPMapper.rewriteMatch(this.getTenantId(), fm.getMatch());
                approvedActions.addAll(lUtils.unsetLinkFields());
            } else {
                SwitchRoute.log
                .warn("Cannot retrieve the virtual link between ports {} {}. Dropping packet...",
                        dstPort, this.getSrcPort());
                return;
            }
        } else {
            approvedActions.addAll(IPMapper.prependRewriteActions(
                    this.getTenantId(), fm.getMatch()));
        }

        fm.getMatch().setInputPort(this.getSrcPort().getPhysicalPortNumber());

        // add the output action with the physical outPort (srcPort of the
        // route)
        if (this.getSrcPort().getPhysicalPortNumber() != this.getPathSrcPort()
                .getPortNumber()) {
            approvedActions.add(new OFActionOutput(this.getPathSrcPort()
                    .getPortNumber()));
        } else {
            approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT
                    .getValue()));
        }

        fm.setCommand(OFFlowMod.OFPFC_MODIFY);
        fm.setActions(approvedActions);
        int actLenght = 0;
        for (final OFAction act : approvedActions) {
            actLenght += act.getLengthU();
        }
        fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + actLenght);
        this.getSrcSwitch().sendMsg(fm, this.getSrcSwitch());
        SwitchRoute.log.debug("Sending big-switch route first fm to sw {}: {}",
                this.getSrcSwitch().getName(), fm);
    }

    /**
     * Registers switch route in persistent storage.
     * 
     * @param path
     *            list of PhysicalLinks
     * @param priority
     *            the priority of this route.
     */
    public void register(final List<PhysicalLink> path, final byte priority) {
        this.state.register(this, path);
    }

    @Override
    public void unregister() {
        this.state.unregister(this);
    }

    @Override
    public Map<String, Object> getDBIndex() {
        final Map<String, Object> index = new HashMap<String, Object>();
        index.put(TenantHandler.TENANT, this.getTenantId());
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
            final Map<String, Object> dbObject = new HashMap<String, Object>();
            dbObject.put(TenantHandler.VDPID, this.getSwitchId());
            dbObject.put(TenantHandler.SRC_PORT, this.srcPort.getPortNumber());
            dbObject.put(TenantHandler.DST_PORT, this.dstPort.getPortNumber());
            dbObject.put(TenantHandler.PRIORITY, this.priority);
            dbObject.put(TenantHandler.ROUTE, this.routeId);
            // Build path list
            final List<PhysicalLink> links = OVXMap.getInstance()
					.getRoute(this);
            final List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
            for (final PhysicalLink link : links) {
                final Map<String, Object> obj = link.getDBObject();
                // Physical link id's are meaningless when restarting OVX,
                // as these depend on the order in which the links are
                // discovered
                obj.remove(TenantHandler.LINK);
                path.add(obj);
            }
            dbObject.put(TenantHandler.PATH, path);
            return dbObject;
        } catch (final LinkMappingException e) {
            return null;
        }
    }

    /**
     * Tries to switch this route to a backup path, and updates mappings to
     * "correct" string of PhysicalLinks to use for this SwitchRoute.
     *
     * @param plink
     *            the failed PhysicalLink
     * @return true if successful
     */
    @Override
    public boolean tryRecovery(final Component plink) {
        return this.state.tryRecovery(this, (PhysicalLink) plink);
    }

    /**
     * Gets the set of physical links that make up both primary and backup paths
     * for the switch route.
     *
     * @return set of physical links
     */
    public HashSet<PhysicalLink> getLinks() {
        final HashSet<PhysicalLink> list = new HashSet<PhysicalLink>();
        try {
            list.addAll(OVXMap.getInstance().getRoute(this));
        } catch (final LinkMappingException e) {
            SwitchRoute.log.warn("Unable to fetch primary route : {}",
					e.getMessage());
        }
        for (final List<PhysicalLink> links : this.backupRoutes.values()) {
            list.addAll(links);
        }

        return list;
    }

    /**
     * Attempts to switch this route back to the original path.
     *
     * @param plink
     *            physical link that was restored
     * @return true for success, false otherwise
     */
    @Override
    public boolean tryRevert(final Component plink) {
        return this.state.tryRevert(this, (PhysicalLink) plink);
    }

    @Override
    public boolean boot() {
        return this.state.boot(this);
    }

    @Override
    public boolean tearDown() {
        return this.state.teardown(this);
    }

}
