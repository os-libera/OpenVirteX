/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.routing;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.*;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

import java.util.*;

public class SwitchRoute extends Link<OVXPort, PhysicalSwitch> implements Persistable {
    private static Logger log = LogManager.getLogger(SwitchRoute.class.getName());
    /**
     * Database keyword for switch routes.
     */
    public static final String DB_KEY = "routes";
    // Unique (for this switch) route identifier
    private int routeId;
    // Parent virtual switch
    private OVXSwitch sw;
    private byte priority;
    private final TreeMap<Byte, List<PhysicalLink>> backupRoutes;
    private final TreeMap<Byte, List<PhysicalLink>> unusableRoutes;
    // A reference to the PhysicalPort at the start of the path
    private PhysicalPort inPort;
    // A reference to the PhysicalPort at the start of the path
    private PhysicalPort outPort;

    /**
     * Instantiates a new switch route for the given switch between
     * two ports, and assigns a route ID and priority value.
     *
     * @param sw the virtual switch
     * @param in the ingress port
     * @param out the egress port
     * @param routeid the route ID
     * @param priority the priority value
     */
    public SwitchRoute(final OVXSwitch sw, final OVXPort in, final OVXPort out,
                       final int routeid, final byte priority) {
        super(in, out);
        this.sw = sw;
        this.routeId = routeid;
        this.priority = priority;
        this.backupRoutes = new TreeMap<>();
        this.unusableRoutes = new TreeMap<>();
    }

    /**
     * Sets the switch-unique identifier of this route.
     *
     * @param routeid the route ID
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
        return priority;
    }

    /**
     * Sets the priority value of the switch route.
     *
     * @param priority the priority value
     */
    public void setPriority(byte priority) {
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
     * Sets the ingress port of the switch route to the given
     * physical port.
     *
     * @param start the source physical port
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
     * Sets the egress port of the switch route to the given
     * physical port.
     *
     * @param end the destination physical port
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

    /**
     * Adds a backup route with given priority value and path given as
     * list of physical links.
     *
     * @param priority the priority value
     * @param physicalLinks the backup route
     */
    public void addBackupRoute(Byte priority,
                               final List<PhysicalLink> physicalLinks) {
        this.backupRoutes.put(priority, physicalLinks);
    }

    /**
     * Replaces the current primary route by the new route given as
     * a list of physical links and priority value.
     *
     * @param priority the priority value
     * @param physicalLinks the new route
     */
    public void replacePrimaryRoute(Byte priority,
                                    final List<PhysicalLink> physicalLinks) {
        // Save the current path in the backup Map
        try {
            this.addBackupRoute(this.getPriority(), OVXMap.getInstance()
                    .getRoute(this));
        } catch (LinkMappingException e) {
            SwitchRoute.log.error(
                    "Unable to retrieve the list of physical link from the"
                            + "OVXMap associated to the big-switch route {}",
                    this.getRouteId());
        }

        this.switchPath(physicalLinks, priority);
    }

    @Override
    public String toString() {
        return "routeId: " + this.routeId + " dpid: " + this.getSwitchId()
                + " inPort: " + this.srcPort == null ? "" : this.srcPort
                .toString() + " outPort: " + this.dstPort == null ? ""
                : this.dstPort.toString();
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
     * @param physicalLinks the new path
     * @param priority the priority of the new path
     */
    public void switchPath(List<PhysicalLink> physicalLinks, byte priority) {
        // Register the new path as primary path in the OVXMap
        OVXMap.getInstance().removeRoute(this);
        OVXMap.getInstance().addRoute(this, physicalLinks);
        // Set the route priority to the new one
        this.setPriority(priority);

        int counter = 0;
        SwitchRoute.log.info(
                "Virtual network {}: switching all existing flow-mods crossing"
                        + "the big-switch {} route {} between ports ({},{}) to the new path: {}",
                this.getTenantId(), this.getSrcPort().getParentSwitch()
                        .getSwitchName(), this.getRouteId(), this.getSrcPort()
                        .getPortNumber(), this.getDstPort().getPortNumber(),
                physicalLinks);
        Collection<OVXFlowMod> flows = this.getSrcPort().getParentSwitch()
                .getFlowTable().getFlowTable();
        for (OVXFlowMod fe : flows) {
            for (OFAction act : fe.getFlowMod().getActions()) {

                //check MatchField.IN_PORT
                if(fe.getFlowMod().getMatch().get(MatchField.IN_PORT) != null) {

                    if (act.getType() == OFActionType.OUTPUT
                            && fe.getFlowMod().getMatch().get(MatchField.IN_PORT).getShortPortNumber()
                            == this.getSrcPort().getPortNumber()
                            && ((OFActionOutput) act).getPort().getShortPortNumber()
                            == this.getDstPort().getPortNumber()) {

                        SwitchRoute.log.info(
                                "Virtual network {}, switch {}, route {} between ports {}-{}: switch fm {}",
                                this.getTenantId(), this.getSrcPort()
                                        .getParentSwitch().getSwitchName(), this
                                        .getRouteId(), this.getSrcPort()
                                        .getPortNumber(), this.getDstPort()
                                        .getPortNumber(), fe);
                        counter++;

                        OVXFlowMod fm = fe.clone();

                        fe.setOFMessage(fe.getFlowMod().createBuilder()
                                .setCookie(U64.of(((OVXFlowTable)this.getSrcPort().getParentSwitch().getFlowTable()).getCookie(fe, true)))
                                .build()
                        );

                        //fm.setCookie(U64.of(((OVXFlowTable2)this.getSrcPort().getParentSwitch().getFlowTable()).getCookie(fe, true)));

                        this.generateRouteFMs(fm);
                        this.generateFirstFM(fm);

                    }
                }
            }
        }

        log.info(
                "Virtual network {}, switch {}, route {} between ports {}-{}: {} flow-mod switched to the new path",
                this.getTenantId(), this.getSrcPort().getParentSwitch()
                        .getSwitchName(), this.getRouteId(), this.getSrcPort()
                        .getPortNumber(), this.getDstPort().getPortNumber(),
                counter);
    }

    /**
     * Generates and installs all flow mods needed to bring up switch route,
     * base an a given controller-generated flow mod.
     *
     * @param fm the virtual flow mod
     */
    public void generateRouteFMs(final OVXFlowMod fm) {
        // This list includes all the actions that have to be applied at the end
        // of the route
        final LinkedList<OFAction> outActions = new LinkedList<OFAction>();

        if (this.getDstPort().isEdge()) {
            outActions.addAll(IPMapper.prependUnRewriteActions(fm.getFlowMod().getMatch()));
        } else {
            final OVXLink link = this.getDstPort().getLink().getOutLink();
            Integer linkId = link.getLinkId();
            Integer flowId = 0;
            try {
                flowId = OVXMap
                        .getInstance()
                        .getVirtualNetwork(this.getTenantId())
                        .getFlowManager()
                        .storeFlowValues(
                                fm.getFlowMod().getMatch().get(MatchField.ETH_SRC).getBytes(),
                                fm.getFlowMod().getMatch().get(MatchField.ETH_DST).getBytes()
                        );
                link.generateLinkFMs(fm.clone(), flowId);
                outActions.addAll(
                        new OVXLinkUtils(
                                this.getTenantId(),
                                linkId,
                                flowId
                        ).setLinkFields(fm.getOFMessage().getVersion()));
            } catch (IndexOutOfBoundException e) {
                SwitchRoute.log.error(
                        "Too many host to generate the flow pairs in this virtual network {}. "
                                + "Dropping flow-mod {} ", this.getTenantId(),
                        fm);
            } catch (NetworkMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if(fm.getFlowMod().getMatch().get(MatchField.ETH_TYPE) == EthType.IPv4) {
            fm.modifyMatch(
                    IPMapper.rewriteMatch(
                            this.getSrcPort().getTenantId(),
                            fm.getFlowMod().getMatch()
                    )
            );
        }

        PhysicalPort inPort = null;
        PhysicalPort outPort = null;

        fm.setOFMessage(fm.getFlowMod().createBuilder()
                .setBufferId(OFBufferId.NO_BUFFER)
                .build()
        );

        final SwitchRoute route = ((OVXBigSwitch) this.getSrcPort()
                .getParentSwitch()).getRoute(this.getSrcPort(),
                this.getDstPort());
        LinkedList<PhysicalLink> reverseLinks = new LinkedList<>();
        try {
            for (final PhysicalLink phyLink : OVXMap.getInstance().getRoute(
                    route)) {
                reverseLinks.add(new PhysicalLink(phyLink.getDstPort(), phyLink
                        .getSrcPort()));
            }
        } catch (LinkMappingException e) {
            SwitchRoute.log.warn("Could not fetch route : {}", e);
            return;
        }
        Collections.reverse(reverseLinks);

        for (final PhysicalLink phyLink : reverseLinks) {
            if (outPort != null) {
                inPort = phyLink.getSrcPort();

                ArrayList<OFAction> actionList = new ArrayList<OFAction>();

                OFActionOutput actionOutput = OFFactories.getFactory(fm.getOFMessage().getVersion())
                        .actions().buildOutput()
                        .setPort(OFPort.of(outPort.getPortNumber()))
                        .setMaxLen(0xffff)
                        .build();

                actionList.add(actionOutput);

                fm.modifyMatch(fm.getFlowMod().getMatch().createBuilder()
                        .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPortNumber()))
                        .build());

                fm.setOFMessage(fm.getFlowMod().createBuilder()
                        .setActions(actionList)
                        .build());

                phyLink.getSrcPort().getParentSwitch()
                        .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
                SwitchRoute.log.debug(
                        "Sending big-switch route intermediate fm to sw {}: {}",
                        phyLink.getSrcPort().getParentSwitch().getName(), fm);

            } else {

                fm.modifyMatch(fm.getFlowMod().getMatch().createBuilder()
                        .setExact(MatchField.IN_PORT, OFPort.of(phyLink.getSrcPort().getPortNumber()))
                        .build());

                OFActionOutput actionOutput = OFFactories.getFactory(fm.getOFMessage().getVersion())
                        .actions().buildOutput()
                        .setPort(OFPort.of(this.getDstPort().getPhysicalPortNumber()))
                        .setMaxLen(0xffff)
                        .build();

                outActions.add(actionOutput);

                fm.setOFMessage(fm.getFlowMod().createBuilder()
                        .setActions(outActions)
                        .build());

                phyLink.getSrcPort().getParentSwitch()
                        .sendMsg(fm, phyLink.getSrcPort().getParentSwitch());
                SwitchRoute.log.debug("Sending big-switch route last fm to sw {}: {}",
                        phyLink.getSrcPort().getParentSwitch().getName(), fm);
            }
            outPort = phyLink.getDstPort();
        }

        // TODO: With POX we need to put a timeout between this flows and the
        // first flowMod. Check how to solve
        try {
            Thread.sleep(5);
        } catch (final InterruptedException e1) {
            SwitchRoute.log.warn("Timeout failed, might be a problem for POX controller: {}", e1);
        }
    }

    /**
     * Generates and installs flow mod on the first physical switch of a switch route,
     * based an a controller-generated flow mod.
     *
     * @param fm the virtual flow mod
     */
    private void generateFirstFM(OVXFlowMod fm) {
        fm.setOFMessage(fm.getFlowMod().createBuilder()
                .setBufferId(OFBufferId.NO_BUFFER)
                .build()
        );

        final List<OFAction> approvedActions = new LinkedList<OFAction>();
        if (this.getSrcPort().isLink()) {
            OVXPort dstPort = null;
            try {
                dstPort = OVXMap.getInstance()
                        .getVirtualNetwork(this.getTenantId())
                        .getNeighborPort(this.getSrcPort());
            } catch (NetworkMappingException e) {
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
                            .getFlowId(
                                    fm.getFlowMod().getMatch().get(MatchField.ETH_SRC).getBytes(),
                                    fm.getFlowMod().getMatch().get(MatchField.ETH_DST).getBytes()
                            );
                } catch (NetworkMappingException e) {
                    SwitchRoute.log.warn(
                            "Error retrieving the network with id {} for flowMod {}. Dropping packet...",
                            this.getTenantId(), fm);
                    return;
                } catch (DroppedMessageException e) {
                    SwitchRoute.log.warn(
                            "Error retrieving flowId in network with id {} for flowMod {}."
                                    + "Dropping packet...", this.getTenantId(), fm);
                    return;
                } catch (IndexOutOfBoundException e) {
                    log.error(
                            "Too many host to generate the flow pairs in this virtual network {}. "
                                    + "Dropping flow-mod {} ",
                            sw.getTenantId(), fm);
                    return;
                }
                OVXLinkUtils lUtils = new OVXLinkUtils(this.getTenantId(), link.getLinkId(), flowId);
                fm.modifyMatch(lUtils.rewriteMatch(fm.getFlowMod().getMatch()));
                fm.modifyMatch(IPMapper.rewriteMatch(this.getTenantId(), fm.getFlowMod().getMatch()));

                approvedActions.addAll(lUtils.unsetLinkFields(false, false, fm.getOFMessage().getVersion()));
            } else {
                SwitchRoute.log.warn(
                        "Cannot retrieve the virtual link between ports {} {}. Dropping packet...",
                        dstPort, this.getSrcPort());
                return;
            }
        } else {
            log.info("prependRewriteActions");
            approvedActions.addAll(IPMapper.prependRewriteActions(
                    this.getTenantId(), fm.getFlowMod().getMatch()));
        }

        fm.modifyMatch(fm.getFlowMod().getMatch().createBuilder()
                .setExact(MatchField.IN_PORT, OFPort.of(this.getSrcPort().getPhysicalPortNumber()))
                .build()
        );



        // add the output action with the physical outPort (srcPort of the
        // route)
        if (this.getSrcPort().getPhysicalPortNumber() != this.getPathSrcPort()
                .getPortNumber()) {

            approvedActions.add(OFFactories.getFactory(fm.getOFMessage().getVersion())
                    .actions().buildOutput()
                    .setPort(OFPort.of(this.getPathSrcPort().getPortNumber()))
                    .setMaxLen(0xffff)
                    .build());
        } else {
            approvedActions.add(OFFactories.getFactory(fm.getOFMessage().getVersion())
                    .actions().buildOutput()
                    .setPort(OFPort.IN_PORT)
                    .setMaxLen(0xffff)
                    .build());
        }


        fm.setOFMessage(fm.getFlowMod().createBuilder()
                .setActions(approvedActions)
                .build());

        this.getSrcSwitch().sendMsg(fm, this.getSrcSwitch());
        SwitchRoute.log.debug("Sending big-switch route first fm to sw {}: {}", this
                .getSrcSwitch().getName(), fm);
    }

    /**
     * Registers switch route in persistent storage.
     */
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
            Map<String, Object> dbObject = new HashMap<String, Object>();
            dbObject.put(TenantHandler.VDPID, this.getSwitchId());
            dbObject.put(TenantHandler.SRC_PORT, this.srcPort.getPortNumber());
            dbObject.put(TenantHandler.DST_PORT, this.dstPort.getPortNumber());
            dbObject.put(TenantHandler.PRIORITY, this.priority);
            dbObject.put(TenantHandler.ROUTE, this.routeId);
            // Build path list
            List<PhysicalLink> links = OVXMap.getInstance().getRoute(this);
            List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
            for (PhysicalLink link : links) {
                Map<String, Object> obj = link.getDBObject();
                // Physical link id's are meaningless when restarting OVX,
                // as these depend on the order in which the links are
                // discovered
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
     * Tries to switch this route to a backup path, and updates mappings to
     * "correct" string of PhysicalLinks to use for this SwitchRoute.
     *
     * @param plink the failed PhysicalLink
     * @return true if successful
     */
    public boolean tryRecovery(PhysicalLink plink) {
        log.info(
                "Try recovery for virtual network {} big-switch {} internal route {} between ports"
                        + "({},{}) in virtual network {} ",
                this.getTenantId(), this.getSrcPort().getParentSwitch()
                        .getSwitchName(), this.routeId, this.getSrcPort()
                        .getPortNumber(), this.getDstPort().getPortNumber(),
                this.getTenantId());
        if (this.backupRoutes.size() > 0) {
            try {
                List<PhysicalLink> unusableLinks = new ArrayList<>(OVXMap.getInstance().getRoute(this));
                Collections.copy(unusableLinks,
                        OVXMap.getInstance().getRoute(this));
                this.unusableRoutes.put(this.getPriority(), unusableLinks);
            } catch (LinkMappingException e) {
                log.warn("No physical Links mapped to SwitchRoute? : {}", e);
                return false;
            }
            byte priority = this.backupRoutes.lastKey();
            List<PhysicalLink> phyLinks = this.backupRoutes.get(priority);
            this.switchPath(phyLinks, priority);
            this.backupRoutes.remove(priority);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the set of physical links that make up both primary and backup paths for the switch route.
     *
     * @return set of physical links
     */
    public Set<PhysicalLink> getLinks() {

        Set<PhysicalLink> list = new HashSet<PhysicalLink>();
        try {
            list.addAll(OVXMap.getInstance().getRoute(this));
        } catch (LinkMappingException e) {
            log.warn("Unable to fetch primary route : {}", e.getMessage());
        }
        for (List<PhysicalLink> links : backupRoutes.values()) {
            list.addAll(links);
        }

        return list;
    }

    /**
     * Attempts to switch this route back to the original path.
     *
     * @param plink physical link that was restored
     * @return true for success, false otherwise
     */
    public boolean tryRevert(PhysicalLink plink) {
        Iterator<Byte> it = this.unusableRoutes.descendingKeySet().iterator();
        while (it.hasNext()) {
            Byte curPriority = it.next();
            if (this.unusableRoutes.get(curPriority).contains(plink)) {
                log.info(
                        "Reactivate all inactive paths for virtual network {} big-switch {}"
                                + "internal route {} between ports ({},{}) in virtual network {} ",
                        this.getTenantId(), this.getSrcPort().getParentSwitch()
                                .getSwitchName(), this.routeId, this
                                .getSrcPort().getPortNumber(), this
                                .getDstPort().getPortNumber(), this
                                .getTenantId());

                if (U8.f(this.getPriority()) >= U8.f(curPriority)) {
                    this.backupRoutes.put(curPriority,
                            this.unusableRoutes.get(curPriority));
                } else {

                    try {
                        List<PhysicalLink> backupLinks = new ArrayList<>(OVXMap.getInstance().getRoute(this));
                        Collections.copy(backupLinks, OVXMap.getInstance()
                                .getRoute(this));
                        this.backupRoutes.put(this.getPriority(), backupLinks);
                        this.switchPath(this.unusableRoutes.get(curPriority),
                                curPriority);
                    } catch (LinkMappingException e) {
                        log.warn(
                                "No physical Links mapped to SwitchRoute? : {}",
                                e);
                        return false;
                    }
                }
                it.remove();
            }
        }
        return true;
    }

}
