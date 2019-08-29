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
package net.onrc.openvirtex.elements.datapath;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Persistable;

import net.onrc.openvirtex.elements.datapath.role.RoleManager.Role;
import net.onrc.openvirtex.elements.datapath.role.RoleManager;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.*;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;


import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.LRULinkedHashMap;

/**
 * The base virtual switch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> implements Persistable {


    private static Logger log = LogManager.getLogger(OVXSwitch.class.getName());

    /**
     * Datapath description string.
     * TODO: should this be made specific per type of virtual switch?
     */
    public static final String DPDESCSTRING = "Virtual Switch";
    protected static int bufferDimension = 4096;
    protected Integer tenantId = 0;
    // default in spec is 128
    protected Short missSendLen = 128;
    protected boolean isActive = false;
    //protected OVXSwitchCapabilities capabilities;
    protected OVXSwitchCapabilities capabilities;
    // The backoff counter for this switch when unconnected
    private AtomicInteger backOffCounter = null;
    protected LRULinkedHashMap<Integer, OVXPacketIn> bufferMap;

    private AtomicInteger bufferId = null;
    private final BitSetIndex portCounter;
    protected FlowTable flowTable;
    // Used to save which channel the message came in on
    private final XidTranslator<Channel> channelMux;
    /**
     * Role Manager. Saves all role requests coming from each controller. It is
     * also responsible for permitting or denying certain operations based on
     * the current role of a controller.
     */
    private final RoleManager roleMan;

    //private OFFeaturesReply ofFeaturesReply;

    /**
     * Instantiates a new OVX switch.
     *
     * @param switchId the switch id
     * @param tenantId the tenant id
     */
    protected OVXSwitch(final Long switchId, final Integer tenantId) {
        super(switchId);
        this.tenantId = tenantId;
        this.missSendLen = 0;
        this.isActive = false;
        this.capabilities = new OVXSwitchCapabilities();
        this.backOffCounter = new AtomicInteger();
        this.resetBackOff();
        this.bufferMap = new LRULinkedHashMap<Integer, OVXPacketIn>(
                OVXSwitch.bufferDimension);
        this.portCounter = new BitSetIndex(IndexType.PORT_ID);
        this.bufferId = new AtomicInteger(1);
        this.flowTable = new OVXFlowTable(this);
        this.roleMan = new RoleManager();
        this.channelMux = new XidTranslator<Channel>();
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
     * Gets the miss send len.
     *
     * @return the miss send len
     */
    public short getMissSendLen() {
        return this.missSendLen;
    }

    /**
     * Sets the miss send len.
     *
     * @param missSendLen
     *            the miss send len
     * @return true, if successful
     */
    public boolean setMissSendLen(final Short missSendLen) {
        this.missSendLen = missSendLen;
        return true;
    }

    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    public boolean isActive() {
        return this.isActive;
    }

    /**
     * Sets the active.
     *
     * @param isActive
     *            the new active
     */
    public void setActive(final boolean isActive) {
//        this.log.info("setActive = " + isActive);
        this.isActive = isActive;
    }

    /**
     * Gets the physical port number.
     *
     * @param ovxPortNumber
     *            the ovx port number
     * @return the physical port number
     */
    public Short getPhysicalPortNumber(final Short ovxPortNumber) {
        return this.portMap.get(ovxPortNumber).getPhysicalPortNumber();
    }

    /**
     * Resets the backoff counter.
     */
    public void resetBackOff() {
        this.backOffCounter.set(-1);
    }

    /**
     * Increments the backoff counter.
     *
     * @return the backoff counter
     */
    public int incrementBackOff() {
        return this.backOffCounter.incrementAndGet();
    }

    /**
     * Gets the next available port number.
     *
     * @return the port number
     * @throws IndexOutOfBoundException if no more port numbers are available
     */
    public short getNextPortNumber() throws IndexOutOfBoundException {
        return this.portCounter.getNewIndex().shortValue();
    }

    /**
     * Releases the given port number so it can be reused.
     *
     * @param portNumber the port number
     */
    public void relesePortNumber(short portNumber) {

        this.portCounter.releaseIndex((int) portNumber);
    }

    /**
     * Adds a default OpenFlow port to the give list of physical ports.
     *
     * @param ports the list of ports
     */

    protected void addDefaultPort(final LinkedList<OFPortDesc> ports) {
        Set<OFPortConfig> config = new HashSet<OFPortConfig>();
        config.add(OFPortConfig.PORT_DOWN);

        Set<OFPortState> state = new HashSet<OFPortState>();
        state.add(OFPortState.LINK_DOWN);

        final byte[] addr = {(byte) 0xA4, (byte) 0x23, (byte) 0x05,
                (byte) 0x00, (byte) 0x00, (byte) 0x00};

        final OFPortDesc port = ofFactory.buildPortDesc()
                .setPortNo(OFPort.LOCAL)
                .setName("OVX Local Port")      //max langth 16
                .setConfig(config)
                .setState(state)
                .setHwAddr(MacAddress.of(addr))
                .build();

        ports.add(port);
    }

    /**
     * Registers switch in the mapping and adds it to persistent storage.
     *
     * @param physicalSwitches
     */
    public void register(final List<PhysicalSwitch> physicalSwitches) {
        // OVXSwitch는 동일한 OpenFlow Version을 지원하는 PhysicalSwitch에 생성된다
        //log.info("OVXSwitch[" + this.getSwitchId() + "] is made in " + )
        this.setOfVersion(physicalSwitches.get(0).getOfVersion());

        this.map.addSwitches(physicalSwitches, this);

        DBManager.getInstance().save(this);
    }

    /**
     * Unregisters switch from persistent storage, from the mapping,
     * and removes all virtual elements that rely on this switch.
     */
    public void unregister() {
        DBManager.getInstance().remove(this);
        this.isActive = false;
        if (this.getPorts() != null) {
            OVXNetwork net;
            try {
                net = this.getMap().getVirtualNetwork(this.tenantId);
            } catch (NetworkMappingException e) {
                log.error(
                        "Error retrieving the network with id {}. Unregister for OVXSwitch {}"
                                + "not fully done!", this.getTenantId(),
                        this.getSwitchName());
                return;
            }
            final Set<Short> portSet = new TreeSet<Short>(this.getPorts().keySet());
            for (final Short portNumber : portSet) {
                final OVXPort port = this.getPort(portNumber);
                if (port.isEdge()) {
                    Host h = net.getHost(port);
                    if (h != null) {
                        net.getHostCounter().releaseIndex(h.getHostId());
                    }
                } else {
                    net.getLinkCounter().releaseIndex(
                            port.getLink().getInLink().getLinkId());
                }
                port.unregister();
            }
        }
        // remove the switch from the map
        try {
            this.map.getVirtualNetwork(this.tenantId).removeSwitch(this);
        } catch (NetworkMappingException e) {
            log.warn(e.getMessage());
        }

        cleanUpFlowMods(false);

        this.map.removeVirtualSwitch(this);
        this.tearDown();
    }

    private void cleanUpFlowMods(boolean isOk) {
        log.info("Cleaning up flowmods");
        List<PhysicalSwitch> physicalSwitches;
        try {
            physicalSwitches = this.map.getPhysicalSwitches(this);
        } catch (SwitchMappingException e) {
            if (!isOk) {
                log.warn(
                        "Failed to cleanUp flowmods for tenant {} on switch {}",
                        this.tenantId, this.getSwitchName());
            }
            return;
        }
        for (PhysicalSwitch sw : physicalSwitches) {
            sw.cleanUpTenant(this.tenantId, (short) 0);
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
        return Switch.DB_KEY;
    }

    @Override
    public String getDBName() {
        return DBManager.DB_VNET;
    }

    @Override
    public Map<String, Object> getDBObject() {
        Map<String, Object> dbObject = new HashMap<String, Object>();
        dbObject.put(TenantHandler.VDPID, this.switchId);
        List<Long> switches = new ArrayList<Long>();
        try {
            for (PhysicalSwitch sw : this.map.getPhysicalSwitches(this)) {
                switches.add(sw.getSwitchId());
            }
        } catch (SwitchMappingException e) {
            return null;
        }
        dbObject.put(TenantHandler.DPIDS, switches);
        return dbObject;
    }

    @Override
    public void tearDown() {
        this.isActive = false;

        roleMan.shutDown();

        cleanUpFlowMods(true);
        for (OVXPort p : getPorts().values()) {
            if (p.isLink()) {
                p.tearDown();
            }
        }
    }

    /**
     * Generate features reply.
     */
    public void generateFeaturesReply() {
        if(this.ofVersion == OFVersion.OF_10) {
            this.generateFeaturesReplyVer10();
        }else{
            this.generateFeaturesReplyVer13();
            this.generatePortDescStatsReplyVer13();
        }
    }

    public void generateFeaturesReplyVer10() {
        final LinkedList<OFPortDesc> portList = new LinkedList<OFPortDesc>();

        for (final OVXPort ovxPort : this.portMap.values()) {
            portList.add(ovxPort.getOfPort());
        }
        this.addDefaultPort(portList);

        //1.0일때 1.3일때 어떻게 할 것인지 고려해야함
        Set<OFActionType> actionTypeSet = new HashSet<>();
        actionTypeSet.add(OFActionType.OUTPUT);
        actionTypeSet.add(OFActionType.SET_VLAN_VID);
        actionTypeSet.add(OFActionType.SET_VLAN_PCP);
        actionTypeSet.add(OFActionType.STRIP_VLAN);
        actionTypeSet.add(OFActionType.SET_DL_SRC);
        actionTypeSet.add(OFActionType.SET_DL_DST);
        actionTypeSet.add(OFActionType.SET_NW_SRC);
        actionTypeSet.add(OFActionType.SET_NW_DST);
        actionTypeSet.add(OFActionType.SET_NW_TOS);
        actionTypeSet.add(OFActionType.SET_TP_SRC);
        actionTypeSet.add(OFActionType.SET_TP_DST);
        actionTypeSet.add(OFActionType.ENQUEUE);

        final OFFeaturesReply ofReply;

        ofReply = ofFactory.buildFeaturesReply()
                .setDatapathId(DatapathId.of(this.switchId))
                .setPorts(portList)
                .setNBuffers(OVXSwitch.bufferDimension)
                .setNTables((short) 1)
                .setCapabilities(this.capabilities.getOVXSwitchCapabilitiesVer10())
                .setActions(actionTypeSet)
                .setXid(0)
                .build();

        this.setFeaturesReply(ofReply);
    }

    public void generateFeaturesReplyVer13() {
        final OFFeaturesReply ofReply = ofFactory.buildFeaturesReply()
                .setDatapathId(DatapathId.of(this.switchId))
                .setNBuffers(OVXSwitch.bufferDimension)
                .setNTables((short) 1)
                .setCapabilities(this.capabilities.getOVXSwitchCapabilitiesVer13())
                .setXid(0)
                .build();

        this.setFeaturesReply(ofReply);
    }

    public void generatePortDescStatsReplyVer13() {
        final LinkedList<OFPortDesc> portList = new LinkedList<OFPortDesc>();

        for (final OVXPort ovxPort : this.portMap.values()) {
            portList.add(ovxPort.getOfPort());
        }
        this.addDefaultPort(portList);

        final OFPortDescStatsReply ofPortDescStatsReply = ofFactory.buildPortDescStatsReply()
                .setEntries(portList)
                .setXid(0)
                .build();

        this.setPortDescReply(ofPortDescStatsReply);
    }

    /**
     * Boots virtual switch by connecting it to the controller.
     *
     * @return true if successful, false otherwise
     */
    @Override
    public boolean boot() {
        this.generateFeaturesReply();

        final OpenVirteXController ovxController = OpenVirteXController
                .getInstance();
        ovxController.registerOVXSwitch(this);
        this.setActive(true);
        for (OVXPort p : getPorts().values()) {
            if (p.isLink()) {
                p.boot();
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
     */
    @Override
    public String toString() {

        String temp = "SWITCH: switchId: " + this.switchId + " - switchName: "
                + this.switchName + " - isConnected: " + this.isConnected
                + " - tenantId: " + this.tenantId + " - missSendLength: "
                + this.missSendLen + " - isActive: " + this.isActive
                + " - capabilities: ";

        if(this.ofVersion == OFVersion.OF_10) {
            temp = temp + this.capabilities.getOVXSwitchCapabilitiesVer10();
        }else{
            temp = temp + this.capabilities.getOVXSwitchCapabilitiesVer13();
        }

        return temp;
    }

    /**
     * Adds a packet_in to the buffer map and returns a unique buffer ID.
     *
     * @param pktIn the packet_in
     * @return the buffer ID
     */
    public synchronized int addToBufferMap(final OVXPacketIn pktIn) {
        // TODO: this isn't thread safe... fix it
        this.bufferId.compareAndSet(OVXSwitch.bufferDimension, 0);
        this.bufferMap.put(this.bufferId.get(), new OVXPacketIn(pktIn));
        return this.bufferId.getAndIncrement();
    }

    /**
     * Gets a packet_in from a given buffer ID.
     *
     * @param bufId the buffer ID
     * @return packet_in packet
     */
    public OVXPacketIn getFromBufferMap(final Integer bufId) {

        return this.bufferMap.get(bufId);
    }

    /**
     * Gets the flow table.
     *
     * @return the flow table
     */
    public FlowTable getFlowTable() {
        return this.flowTable;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((tenantId == null) ? 0 : tenantId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OVXSwitch)) {
            return false;
        }
        OVXSwitch other = (OVXSwitch) obj;
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        }
        return this.switchId == other.switchId
                && this.tenantId == other.tenantId;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.
     * OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
     */
    @Override
    public void sendMsg(final OVXMessage msg, final OVXSendMsg from) {

        XidPair<Channel> pair = channelMux.untranslate((int)msg.getOFMessage().getXid());
        Channel c = null;

        if (pair != null) {
            msg.setOFMessage(msg.getOFMessage().createBuilder().setXid(pair.getXid()).build());
            c = pair.getSwitch();
        }

        if (this.isConnected && this.isActive) {
            roleMan.sendMsg(msg.getOFMessage(), c);
        } else {
            // TODO: we probably should install a drop rule here.
            log.warn(
                    "Virtual switch {} is not active or is not connected to a controller",
                    switchName);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(final OVXMessage msg, Channel channel) {
        /*
         * Save the channel the msg came in on
         */
        //msg.setXid(channelMux.translate(msg.getXid(), channel));
        msg.setOFMessage(
                msg.getOFMessage().createBuilder()
                        .setXid(channelMux.translate((int)msg.getOFMessage().getXid(), channel))
                        .build()
        );

        //OVXSwitch2.log.error(msg.getOFMessage().toString());

        try {
            /*
             * Check whether this channel (i.e., controller) is permitted to
             * send this msg to the dataplane
             */
            if (this.roleMan.canSend(channel, msg.getOFMessage())) {
                ((Devirtualizable) msg).devirtualize(this);
            } else {
                denyAccess(channel, msg.getOFMessage(), this.roleMan.getRole(channel));
            }
        } catch (final ClassCastException e) {
            OVXSwitch.log.error("Received illegal message: " + msg.getOFMessage().toString());
        } catch (OFParseError ofParseError) {
            ofParseError.printStackTrace();
        }
    }

    @Override
    public void handleRoleIO(OVXMessage msg, Channel channel) {

        Role reqRole = Role.EQUAL;

        if(msg.getOFMessage().getVersion() == OFVersion.OF_10) {
            //need to implement OFVendor Message
        }else{
            OFRoleRequest ofRoleRequest = (OFRoleRequest)msg.getOFMessage();

            switch(ofRoleRequest.getRole()) {
                case ROLE_EQUAL:
                    reqRole = Role.EQUAL;
                    break;
                case ROLE_MASTER:
                    reqRole = Role.MASTER;
                    break;
                case ROLE_SLAVE:
                    reqRole = Role.SLAVE;
                    break;
                case ROLE_NOCHANGE:
                    reqRole = Role.NOCHANGE;
                    break;
            }

            try {
                this.roleMan.setRole(channel, reqRole);
                sendRoleReply(reqRole, (int)msg.getOFMessage().getXid(), channel);
                log.info("Finished handling role for {}",
                        channel.getRemoteAddress());
            } catch (IllegalArgumentException | UnknownRoleException ex) {
                log.warn(ex.getMessage());
            }
        }
    }

    /**
     * Gets a OVXFlowMod out of the map based on the given cookie.
     *
     * @param cookie
     *            the physical cookie
     * @return the virtual flow mod
     * @throws MappingException if the cookie is not found
     */
    public OVXFlowMod getFlowMod(final Long cookie) throws MappingException {
        return this.flowTable.getFlowMod(cookie).clone();
    }

    /**
     * Sets the channel.
     *
     * @param channel the channel
     */
    public void setChannel(Channel channel) {
        this.roleMan.addController(channel);
    }

    /**
     * Removes the given channel.
     *
     * @param channel the channel
     */
    public void removeChannel(Channel channel) {
        this.roleMan.removeChannel(channel);
    }

    /**
     * Removes an entry in the mapping.
     *
     * @param cookie
     * @return The deleted FlowMod
     */
    public OVXFlowMod deleteFlowMod(final Long cookie) {

        return this.flowTable.deleteFlowMod(cookie);
    }

    /**
     * Extracts the vendor-specific (Nicira) role.
     *
     * @param chan the channel
     * @param vendorMessage the vendor message
     * @return the role
     */
/*     private Role extractNiciraRoleRequest(Channel chan, OFExperimenter vendorMessage) {
       OFNiciraControllerRoleRequest nrr = (OFNiciraControllerRoleRequest)vendorMessage;
        OFNiciraControllerRole ncr = nrr.getRole();

        long vendor = nrr.getExperimenter();
        if(vendor != OFNiciraVendorData.NX_VENDOR_ID) {
            return null;
        }

        Role role = null;
        switch(ncr) {
            case ROLE_MASTER:
                role = Role.MASTER;
                break;
            case ROLE_OTHER:
                role = Role.EQUAL;
                break;
            case ROLE_SLAVE:
                role = Role.EQUAL;
                break;
            default:
        }

        if (role == null) {
            String msg = String.format("Controller: [%s], State: [%s], "
                            + "received NX_ROLE_REPLY with invalid role " + "value %d",
                    chan.getRemoteAddress(), this.toString(), nrr.getRole());
            throw new ControllerStateException(msg);
        }*/

        //OFNiciraControllerRoleRequest roleRequest = (OFNiciraControllerRoleRequest)vendorMessage;
        /*long vendor = vendorMessage.getExperimenter();

        if(vendor != OFNiciraVendorData.NX_VENDOR_ID) {
            return null;
        }

        if(!(vendorMessage.getRole() instanceof OFNiciraControllerRole)) {
            return null;
        }

        OFControllerRole role = roleRequest.getRole();
        if (role == null) {
            String msg = String.format("Controller: [%s], State: [%s], "
                            + "received NX_ROLE_REPLY with invalid role " + "value %d",
                    chan.getRemoteAddress(), this.toString(),
                    roleRequest.getRole());
            throw new ControllerStateException(msg);
        }

        return role;
    }*/

    /**
     * Denies access to controller because of role state.
     *
     * @param channel the channel
     * @param m the message
     * @param role the role
     */
    private void denyAccess(Channel channel, OFMessage m, Role role) {
        log.warn(
                "Controller {} may not send message {} because role state is {}",
                channel.getRemoteAddress(), m, role.toString());

        OFMessage e = ofFactory.errorMsgs().buildBadRequestErrorMsg()
                .setCode(OFBadRequestCode.EPERM)
                .build();

        channel.write(Collections.singletonList(e));
    }

    /**
     * Sends a role reply.
     *
     * @param role the role
     * @param xid the transaction ID
     * @param channel the channel on which to send
     */
    private void sendRoleReply(Role role, int xid, Channel channel) {
        OFControllerRole tempRole;

        switch(role)
        {
            case EQUAL:
                tempRole = OFControllerRole.ROLE_EQUAL;
                break;
            case MASTER:
                tempRole = OFControllerRole.ROLE_MASTER;
                break;
            case SLAVE:
                tempRole = OFControllerRole.ROLE_SLAVE;
                break;
            default:
                tempRole = OFControllerRole.ROLE_NOCHANGE;
                break;
        }

        OFRoleReply ofRoleReply = ofFactory.buildRoleReply()
                .setXid((long)xid)
                .setRole(tempRole)
                .build();

        channel.write(Collections.singletonList(ofRoleReply));
    }

    /**
     * Generates a new XID for messages destined for the physical network.
     *
     * @param msg
     *            The OFMessage being translated
     * @param inPort
     *            The ingress port
     * @return the new transaction ID
     */
    public abstract int translate(OVXMessage msg, OVXPort inPort);

    /**
     * Sends a message towards the physical network, via the PhysicalSwitch
     * mapped to this OVXSwitch.
     *
     * @param msg
     *            The OFMessage being translated
     * @param inPort
     *            The ingress port, used to identify the PhysicalSwitch
     *            underlying an OVXBigSwitch. May be null. Sends a message
     *            towards the physical network
     */
    public abstract void sendSouth(OVXMessage msg, OVXPort inPort);

}
