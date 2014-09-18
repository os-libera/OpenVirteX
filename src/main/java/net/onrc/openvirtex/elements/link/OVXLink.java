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
 * Implementation of a virtual link, which adds a unique ID, stores the tenant
 * ID, a priority and a routing algorithm.
 */
public class OVXLink extends Link<OVXPort, OVXSwitch> implements Resilient {

    /**
     * The FSM for a OVXLink. Coupled with OVXPort FSM states.
     */
    enum LinkState {
        INIT {
            protected void initialize(OVXLink link) {
                link.log.debug("Initializing link {}", link);
                link.srcPort.setOutLink(link);
                link.dstPort.setInLink(link);

                try {
                    link.map.getVirtualNetwork(link.tenantId).addLink(link);
                } catch (NetworkMappingException e) {
                    link.log.warn(
                            "No OVXNetwork associated with this link [{}-{}]",
                            link.srcPort.toAP(), link.dstPort.toAP());
                }
            }

            protected void register(final OVXLink link,
                    final List<PhysicalLink> physicalLinks, byte priority) {
                link.log.debug("registering link {}", link);
                link.state = LinkState.INACTIVE;

                // register the primary link in the map
                link.map.removeVirtualLink(link);
                link.map.addLinks(physicalLinks, link);
                link.switchPath(physicalLinks, priority);
                link.srcPort.setEdge(false);
                link.dstPort.setEdge(false);
                DBManager.getInstance().save(link);
            }
        },
        INACTIVE {
            protected boolean boot(OVXLink link) {
                link.log.debug("enabling link {}", link);
                link.state = LinkState.ACTIVE;
                int linkup = ~OFPortState.OFPPS_LINK_DOWN.getValue();
                link.srcPort.setState(link.srcPort.getState() & linkup);
                link.dstPort.setState(link.dstPort.getState() & linkup);
                return true;
            }

            protected void unregister(OVXLink link) {
                link.log.debug("unregistering link {}", link);
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
                    link.log.warn(
                            "[unregister()]: could not remove this link from map \n{}",
                            e.getMessage());
                }
            }

            public boolean tryRevert(OVXLink vlink, PhysicalLink plink) {
                try {
                    if (vlink.unusableLinks.isEmpty()) {
                        return false;
                    }
                    synchronized (vlink.unusableLinks) {
                        Iterator<Byte> it = vlink.unusableLinks.descendingKeySet()
                                .iterator();
                        while (it.hasNext()) {
                            Byte curPriority = it.next();
                            if (vlink.unusableLinks.get(curPriority)
                                    .contains(plink)) {
                                vlink.log
                                .debug("Reactivate all inactive paths for virtual link {} in virtual network {} ",
                                        vlink.linkId, vlink.tenantId);
                                vlink.map.removeVirtualLink(vlink);
                                vlink.map.addLinks(vlink.unusableLinks.get(curPriority), vlink );
                                vlink.switchPath(vlink.map.getPhysicalLinks(vlink), curPriority);
                                vlink.boot();
                                it.remove();
                            }
                        }
                    }
                    return true;
                } catch (LinkMappingException e1) {
                    vlink.log
                    .warn("No physical Links mapped to SwitchRoute? : {}",
                    e1);
                    return false;
                }
            }

            public void generateFMs(OVXLink link, OVXFlowMod fm, Integer flowId) {
                link.generateFlowMods(fm, flowId);
            }
        },
        ACTIVE {
            protected boolean teardown(OVXLink link) {
                link.log.debug("disabling link {}", link);
                link.state = LinkState.INACTIVE;

                setEndStates(link, OFPortState.OFPPS_LINK_DOWN.getValue());
                return true;
            }

            public boolean tryRecovery(OVXLink vlink, PhysicalLink plink) {
                vlink.log.debug(
                        "Try recovery for virtual link {} [id={}, tID={}]",
                        vlink, vlink.linkId, vlink.tenantId);
                /*
                 * store broken link to force re-initialization when we
                 * tryRevert().
                 */
                try {
                    List<PhysicalLink> unusableLinks = new ArrayList<>(
                            vlink.map.getPhysicalLinks(vlink));
                    Collections.copy(unusableLinks,
                            vlink.map.getPhysicalLinks(vlink));
                    vlink.unusableLinks.put(vlink.getPriority(), unusableLinks);
                    /* TODO Check if we need to setcurrent priority to lowest*/
                    //vlink.priority = -1;
                } catch (LinkMappingException e) {
                    vlink.log.warn("No physical Links mapped to OVXLink? : {}",
                            e);
                    return false;
                }
                if (vlink.backupLinks.size() > 0) {
                    /*
                     * remove the existing physicalPath from the map, 
                     * add backup path to map and switch backup path to primary 
                     */
                    byte priority = vlink.backupLinks.lastKey();
                    List<PhysicalLink> phyLinks = vlink.backupLinks
                            .get(priority);
                    vlink.map.removeVirtualLink(vlink);
                    vlink.map.addLinks(phyLinks, vlink);
                    vlink.switchPath(phyLinks, priority);
                    vlink.backupLinks.remove(priority);
                    return true;
                } else
                    return false;
            }

            public void addUnstablePathToBackup(OVXLink link, PhysicalLink plink){
                synchronized(link.unusableLinks){
                    Iterator<Byte> it = link.unusableLinks.keySet().iterator();
                    while(it.hasNext()){
                        byte priority = it.next();
                        if(link.unusableLinks.get(priority) !=null){
                            if(link.unusableLinks.get(priority).contains(plink)){
                                link.backupLinks.put( link.getPriority(), link.unusableLinks.get(priority));
                                it.remove();
                            }
                        }
                    }
                }
            }

            public void addBackupPathToUnstablePath(OVXLink link, PhysicalLink plink){
                synchronized(link.backupLinks){
                    Iterator<Byte> it =  link.backupLinks.keySet().iterator();
                    while (it.hasNext()){
                        byte priority = it.next();
                        if(link.backupLinks.get(priority) !=null){
                            if(link.backupLinks.get(priority).contains(plink)){
                                link.unusableLinks.put( link.getPriority(), link.backupLinks.get(priority));
                                it.remove();
                            }
                        }
                    }
                }
            }

            public void generateFMs(OVXLink link, OVXFlowMod fm, Integer flowId) {
                link.generateFlowMods(fm, flowId);
            }
        },
        STOPPED{
            public void addPathToLink(OVXLink link,
                    List<PhysicalLink> physicalLinks, byte priority) {
                link.log.debug("Cannot add link {} while status={}", link,
                        link.state);
            }

        };

        /**
         * Registers this OVXLink with its endpoint OVXPorts. and the
         * OVXNetwork. This method must be called before register() to fully
         * initialize this link.
         *
         * @param link
         *            this OVXLink
         */
        protected void initialize(final OVXLink link) {
            link.log.debug("Cannot initialize link {} while status={}", link,
                    link.state);
        }

        /**
         * Registers this link with the global OVXMap and sets up backups, if
         * any, for resiliency.
         *
         * @param link
         *            this OVXLink
         * @param physicalLinks
         *            the PhysicalLinks that this VLink maps to.
         * @param priority
         *            the priority of this OVXLink
         */
        protected void register(final OVXLink link,
                final List<PhysicalLink> physicalLinks, byte priority) {
            link.log.debug("Cannot register link {} while status={}", link,
                    link.state);
        }

        /**
         * Activates this link by setting the endpoint states to OFPPS_LINK_DOWN
         * = 0.
         *
         * @param link
         *            the OVXLink
         * @return true if link is successfully enabled.
         */
        protected boolean boot(OVXLink link) {
            link.log.debug("Cannot boot link {} while status={}", link,
                    link.state);
            return false;
        }

        /**
         * Disables this link by setting the endpoint states to OFPPS_LINK_DOWN
         * = 1.
         *
         * @param link
         *            the OVXLink
         * @return true if link is successfully disabled.
         */
        protected boolean teardown(OVXLink link) {
            link.log.debug("Cannot teardown link {} while status={}", link,
                    link.state);
            return false;
        }

        /**
         * Permanently removes this link from the OVXNetwork and global OVXMap.
         *
         * @param link
         *            the OVXLink
         */
        protected void unregister(OVXLink link) {
            link.log.debug("Cannot unregister link {} while status={}", link,
                    link.state);
        }

        /**
         * Sets the OFPortState values of endpoints of this link.
         *
         * @param link
         *            the OVXLink
         * @param nstate
         *            the OFPortState value to set to
         */
        private static void setEndStates(OVXLink link, int nstate) {
            link.srcPort.setState(link.srcPort.getState() | nstate);
            link.dstPort.setState(link.dstPort.getState() | nstate);
        }

        /**
         * If PhsicalLink plink fails, attempts to switch the mapping of OVXLink
         * to a backup path not affected by plink. Recovery is considered
         * successful if the OVXLink is able to switch to a backup path.
         *
         * @param ovxLink
         *            the OVXLink
         * @param plink
         *            the PhysicalLink that has failed
         * @return true if recovery succeeds.
         */
        public boolean tryRecovery(OVXLink ovxLink, PhysicalLink plink) {
            return false;
        }

        /**
         * If falied PhsicalLink plink comes back up, attempts to switch the
         * mapping of OVXLink to the initial path. This is considered successful
         * if the OVXLink is able to switch back.
         *
         * @param ovxLink
         *            the OVXLink
         * @param plink
         *            the PhysicalLink that has come back up
         * @return true if switching back succeeds.
         */
        public boolean tryRevert(OVXLink ovxLink, PhysicalLink plink) {
            return false;
        }

        /**
         * Generates FlowMods to install along the path representing this
         * OVXLink.
         *
         * @param ovxLink
         *            the OVXLink
         * @param fm
         *            the OFFlowMod
         * @param flowId
         *            unique ID of the flow
         */
        public void generateFMs(OVXLink ovxLink, OVXFlowMod fm, Integer flowId) {
        }
        /**
         * Add path to vlink. If priority is less than existing path then
         * add path as backup path else set current path to backup and new path 
         * to primary.
         * @param link 
         *          OVXLink
         * @param physicalLinks
         *                  Physical path connecting src and dst port of vlink
         * @param priority
         *              priority of backup path
         */
        public void addPathToLink(OVXLink link,
                List<PhysicalLink> physicalLinks, byte priority) {
            link.backupLink(physicalLinks, priority);
        }
        /**
         * Add unstable path to backup path if unstablePath gets back to normal
         * while backup link is up and running.
         * @param link 
         *          OVXlink
         * @param plink 
         *          Physical link
         */
        public void addUnstablePathToBackup(OVXLink link, PhysicalLink plink){
            }
        /**
         * Add backup path to unstable if plink goes down that correspond to one
         * of the link in backup (as that backup path is no longer stable).
         * @param link
         *          OVXLink
         * @param plink
         *          PhysicalLink
         */
        public void addBackupPathToUnstablePath(OVXLink link, PhysicalLink plink){
        }
    }

    private Logger log = LogManager.getLogger(OVXLink.class.getName());

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
    private Mappable map = null;
    private LinkState state;

    /**
     * Instantiates a new virtual link. Sets its priority to 0.
     *
     * @param linkId
     *            the unique link id
     * @param tenantId
     *            the tenant id
     * @param srcPort
     *            virtual source port
     * @param dstPort
     *            virtual destination port
     * @param alg
     *            routing algorithm for the virtual link
     * @throws PortMappingException
     *             if one of the ports is invalid
     */
    public OVXLink(final Integer linkId, final Integer tenantId,
            final OVXPort srcPort, final OVXPort dstPort, RoutingAlgorithms alg)
            throws PortMappingException {
        super(srcPort, dstPort);
        this.state = LinkState.INIT;
        this.linkId = linkId;
        this.tenantId = tenantId;
        srcPort.setOutLink(this);
        dstPort.setInLink(this);
        this.backupLinks = new TreeMap<>();
        this.unusableLinks = new TreeMap<>();
        this.priority = (byte) 0;
        this.alg = alg;
        this.map = OVXMap.getInstance();
        /*
         * If SPF routing, let RoutingAlgorithm initialize() link, else do it
         * here
         */
        if (this.alg.getRoutingType() != RoutingType.NONE) {
            this.alg.getRoutable().setLinkPath(this);
        } else {
            this.initialize();
        }
        this.srcPort.getPhysicalPort().removeOVXPort(this.srcPort);
        this.srcPort.getPhysicalPort().setOVXPort(this.srcPort);
    }

    /**
     * Gets the unique link id.
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

    /**
     * Gets the priority value.
     *
     * @return the priority value
     */
    public byte getPriority() {
        return priority;
    }

    /**
     * Sets the priority value.
     *
     * @param priority
     *            the priority value
     */
    public void setPriority(byte priority) {
        this.priority = priority;
    }

    /**
     * Gets the routing algorithm.
     *
     * @return the algorithm
     */
    public RoutingAlgorithms getAlg() {
        return this.alg;
    }

    /**
     * Sets the routing algorithm.
     *
     * @param alg
     *            the algorithm
     */
    public void setAlg(final RoutingAlgorithms alg) {
        this.alg = alg;
    }

    /**
     * Pre-registration method independent of this OVXLink's Physical
     * dependencies. Must be called before register().
     */
    public void initialize() {
        this.state.initialize(this);
    }

    /**
     * Register mapping between virtual link and physical path.
     *
     * @param physicalLinks
     *            the path as a list of physical links
     * @param priority
     *            the priority value
     */
    public void register(final List<PhysicalLink> physicalLinks, byte priority) {
        this.state.register(this, physicalLinks, priority);
    }
    public void addBackupPath(final OVXLink link,final List<PhysicalLink> physicalLinks,
            byte priority){
        link.state.addPathToLink(link, physicalLinks, priority);
    }
    private void backupLink(final List<PhysicalLink> physicalLinks,
            byte priority) {

        if (U8.f(this.getPriority()) >= U8.f(priority)) {
            this.backupLinks.put(priority, physicalLinks);
            log.debug(
                    "Add virtual link {} backup path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
                    this.getLinkId(), U8.f(priority), this.srcPort.toAP(),
                    this.dstPort.toAP(), this.getTenantId(), physicalLinks);
        } else {
            try {
                if (map.getPhysicalLinks(this).equals(physicalLinks)){
                    //TODO: should change priority to new one?
                    log.debug("Ignoreing same path with different priority");
                    return;
                }
                this.backupLinks.put(this.getPriority(),
                        map.getPhysicalLinks(this));
                log.debug(
                        "Replace virtual link {} with a new primary path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
                        this.getLinkId(), U8.f(priority), this.srcPort.toAP(),
                        this.dstPort.toAP(), this.getTenantId(), physicalLinks);
                log.debug(
                        "Switch all existing flow-mods crossing the virtual link {} between ports ({}-{}) to new path",
                        this.getLinkId(), this.getSrcPort().toAP(), this
                                .getDstPort().toAP());
            } catch (LinkMappingException e) {
                log.debug(
                        "Create virtual link {} primary path (priority {}) between ports {}-{} in virtual network {}. Path: {}",
                        this.getLinkId(), U8.f(priority), this.srcPort.toAP(),
                        this.dstPort.toAP(), this.getTenantId(), physicalLinks);
            }
            // TODO this should be boot()
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

    /**
     * Switch the link to the given path and priority.
     *
     * @param physicalLinks
     *            the path as a list of physical links
     * @param priority
     *            the priority value
     */
    private void switchPath(List<PhysicalLink> physicalLinks, byte priority) {
        this.setPriority(priority);

        Collection<OVXFlowMod> flows = this.getSrcSwitch().getFlowTable()
                .getFlowTable();
        for (OVXFlowMod fe : flows) {
            for (OFAction act : fe.getActions()) {
                if (act.getType() == OFActionType.OUTPUT) {
                    if (((OFActionOutput) act).getPort() == this.getSrcPort()
                            .getPortNumber()) {
                        try {
                            Integer flowId = this.map
                                    .getVirtualNetwork(this.tenantId)
                                    .getFlowManager()
                                    .storeFlowValues(
                                            fe.getMatch().getDataLayerSource(),
                                            fe.getMatch()
                                                    .getDataLayerDestination());

                            OVXFlowMod fm = fe.clone();
                            fm.setCookie(((OVXFlowTable) this.getSrcPort()
                                    .getParentSwitch().getFlowTable())
                                    .getCookie(fe, true));
                            this.generateLinkFMs(fm, flowId);
                        } catch (IndexOutOfBoundException e) {
                            log.error(
                                    "Too many hosts to generate the flow pairs in this virtual network {}. "
                                            + "Dropping flow-mod {} ",
                                    this.getTenantId(), fe);
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
        dbObject.put(TenantHandler.ALGORITHM, this.alg.getRoutingType()
                .getValue());
        dbObject.put(TenantHandler.BACKUPS, this.alg.getBackups());
        try {
            // Build path list
            List<PhysicalLink> links = map.getPhysicalLinks(this);
            List<Map<String, Object>> path = new ArrayList<Map<String, Object>>();
            for (PhysicalLink link : links) {
                // Physical link id's are meaningless when restarting OVX
                Map<String, Object> obj = link.getDBObject();
                obj.remove(TenantHandler.LINK);
                path.add(obj);
            }
            dbObject.put(TenantHandler.PATH, path);
        } catch (LinkMappingException e) {
            dbObject.put(TenantHandler.PATH, null);
        }
        return dbObject;
    }

    /**
     * Push the flow mod to all the intermediate switches of the virtual link.
     *
     * @param fm
     *            the original flow mod
     * @param flowId
     *            the flow identifier
     */
    public void generateLinkFMs(final OVXFlowMod fm, final Integer flowId) {
        this.state.generateFMs(this, fm, flowId);
    }

    /**
     * Helper function that does FlowMod generation.
     * 
     * @param the
     *            original flow mod
     * @param the
     *            flow identifier
     * @param the
     *            source switch
     */
    private void generateFlowMods(final OVXFlowMod fm, final Integer flowId) {
        /*
         * Change the packet match: 1) change the fields where the virtual link
         * info are stored 2) change the fields where the physical ips are
         * stored
         */
        final OVXLinkUtils lUtils = new OVXLinkUtils(this.tenantId,
                this.linkId, flowId);
        lUtils.rewriteMatch(fm.getMatch());
        long cookie = tenantId;
        fm.setCookie(cookie << 32);

        if (fm.getMatch().getDataLayerType() == Ethernet.TYPE_IPV4) {
            IPMapper.rewriteMatch(this.tenantId, fm.getMatch());
        }

        /*
         * Get the list of physical links mapped to this virtual link, in
         * REVERSE ORDER
         */
        PhysicalPort inPort = null;
        PhysicalPort outPort = null;
        fm.setBufferId(OVXPacketOut.BUFFER_ID_NONE);
        fm.setCommand(OFFlowMod.OFPFC_MODIFY);
        List<PhysicalLink> plinks = new LinkedList<PhysicalLink>();

        try {
            for (final PhysicalLink phyLink : OVXMap.getInstance()
                    .getPhysicalLinks(this)) {
                PhysicalLink nlink = new PhysicalLink(phyLink.getDstPort(),
                        phyLink.getSrcPort());
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
                log.debug("Sending virtual link intermediate fm to sw {}: {}",
                        phyLink.getSrcPort().getParentSwitch().getSwitchName(),
                        fm);
            }
            outPort = phyLink.getDstPort();
        }
        // TODO: With POX we need to put a timeout between this flows and the
        // first flow mod. Check how to solve.
        try {
            Thread.sleep(5);
        } catch (final InterruptedException e) {
            log.warn("Timeout interrupted; might be a problem if you are running POX.");
        }
    }

    /**
     * Tries to switch link to a backup path, and updates mappings to "correct"
     * string of PhysicalLinks to use for this link.
     *
     * @param plink
     *            the failed PhysicalLink
     * @return true if successful, false otherwise
     */
    public boolean tryRecovery(Component plink) {
        return this.state.tryRecovery(this, (PhysicalLink) plink);
    }

    /**
     * Attempts to switch this link back to the original path.
     *
     * @param plink
     *            the restored physical link
     * @return true if successful, false otherwise.
     */
    public boolean tryRevert(Component plink) {
        return this.state.tryRevert(this, (PhysicalLink) plink);
    }
    public void addUnstablePathToBackup(OVXLink link, PhysicalLink plink){
        this.state.addUnstablePathToBackup(link, plink);
        }
    public void addBackupPathToUnstablePath(OVXLink link, PhysicalLink plink){
        this.state.addBackupPathToUnstablePath(link, plink);
    }

    @Override
    public boolean boot() {
        return this.state.boot(this);
    }

}
