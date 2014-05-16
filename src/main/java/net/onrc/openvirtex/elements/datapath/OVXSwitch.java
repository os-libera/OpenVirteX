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
package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Persistable;

import java.util.Set;
import java.util.TreeSet;

import net.onrc.openvirtex.elements.datapath.role.RoleManager;
import net.onrc.openvirtex.elements.datapath.role.RoleManager.Role;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ControllerStateException;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.UnknownRoleException;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.util.LRULinkedHashMap;
import org.openflow.vendor.nicira.OFNiciraVendorData;
import org.openflow.vendor.nicira.OFRoleReplyVendorData;
import org.openflow.vendor.nicira.OFRoleRequestVendorData;

/**
 * The base virtual switch.
 */
public abstract class OVXSwitch extends Switch<OVXPort> implements Persistable {


    private static Logger log = LogManager.getLogger(OVXSwitch.class.getName());

    /**
     * Datapath description string.
     * TODO: should this be made specific per type of virtual switch?
     */
    public static final String DPDESCSTRING = "OpenVirteX Virtual Switch";
    protected static int supportedActions = 0xFFF;
    protected static int bufferDimension = 4096;
    protected Integer tenantId = 0;
    // default in spec is 128
    protected Short missSendLen = 128;
    protected boolean isActive = false;
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
    protected void addDefaultPort(final LinkedList<OFPhysicalPort> ports) {
        final OFPhysicalPort port = new OFPhysicalPort();
        port.setPortNumber(OFPort.OFPP_LOCAL.getValue());
        port.setName("OpenFlow Local Port");
        port.setConfig(1);
        final byte[] addr = {(byte) 0xA4, (byte) 0x23, (byte) 0x05,
                (byte) 0x00, (byte) 0x00, (byte) 0x00};
        port.setHardwareAddress(addr);
        port.setState(1);
        port.setAdvertisedFeatures(0);
        port.setCurrentFeatures(0);
        port.setSupportedFeatures(0);
        ports.add(port);
    }

    /**
     * Registers switch in the mapping and adds it to persistent storage.
     *
     * @param physicalSwitches
     */
    public void register(final List<PhysicalSwitch> physicalSwitches) {
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
            final Set<Short> portSet = new TreeSet<Short>(this.getPorts()
                    .keySet());
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
        final OFFeaturesReply ofReply = new OFFeaturesReply();
        ofReply.setDatapathId(this.switchId);
        final LinkedList<OFPhysicalPort> portList = new LinkedList<OFPhysicalPort>();
        for (final OVXPort ovxPort : this.portMap.values()) {
            final OFPhysicalPort ofPort = new OFPhysicalPort();
            ofPort.setPortNumber(ovxPort.getPortNumber());
            ofPort.setName(ovxPort.getName());
            ofPort.setConfig(ovxPort.getConfig());
            ofPort.setHardwareAddress(ovxPort.getHardwareAddress());
            ofPort.setState(ovxPort.getState());
            ofPort.setAdvertisedFeatures(ovxPort.getAdvertisedFeatures());
            ofPort.setCurrentFeatures(ovxPort.getCurrentFeatures());
            ofPort.setSupportedFeatures(ovxPort.getSupportedFeatures());
            portList.add(ofPort);
        }

        /*
         * Giving the switch a port (the local port) which is set
         * administratively down.
         *
         * Perhaps this can be used to send the packets to somewhere
         * interesting.
         */
        this.addDefaultPort(portList);
        ofReply.setPorts(portList);
        ofReply.setBuffers(OVXSwitch.bufferDimension);
        ofReply.setTables((byte) 1);
        ofReply.setCapabilities(this.capabilities.getOVXSwitchCapabilities());
        ofReply.setActions(OVXSwitch.supportedActions);
        ofReply.setXid(0);
        ofReply.setLengthU(OFFeaturesReply.MINIMUM_LENGTH
                + OFPhysicalPort.MINIMUM_LENGTH * portList.size());

        this.setFeaturesReply(ofReply);
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
        return "SWITCH: switchId: " + this.switchId + " - switchName: "
                + this.switchName + " - isConnected: " + this.isConnected
                + " - tenantId: " + this.tenantId + " - missSendLength: "
                + this.missSendLen + " - isActive: " + this.isActive
                + " - capabilities: "
                + this.capabilities.getOVXSwitchCapabilities();
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
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {

        XidPair<Channel> pair = channelMux.untranslate(msg.getXid());
        Channel c = null;
        if (pair != null) {
            msg.setXid(pair.getXid());
            c = pair.getSwitch();
        }

        if (this.isConnected && this.isActive) {
            roleMan.sendMsg(msg, c);
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
    public void handleIO(final OFMessage msg, Channel channel) {
        /*
         * Save the channel the msg came in on
         */
        msg.setXid(channelMux.translate(msg.getXid(), channel));
        try {
            /*
             * Check whether this channel (i.e., controller) is permitted to
             * send this msg to the dataplane
             */

            if (this.roleMan.canSend(channel, msg)) {
                ((Devirtualizable) msg).devirtualize(this);
            } else {
                denyAccess(channel, msg, this.roleMan.getRole(channel));
            }
        } catch (final ClassCastException e) {
            OVXSwitch.log.error("Received illegal message: " + msg);
        }
    }

    @Override
    public void handleRoleIO(OFVendor msg, Channel channel) {

        Role role = extractNiciraRoleRequest(channel, msg);
        try {
            this.roleMan.setRole(channel, role);
            sendRoleReply(role, msg.getXid(), channel);
            log.info("Finished handling role for {}",
                    channel.getRemoteAddress());
        } catch (IllegalArgumentException | UnknownRoleException ex) {
            log.warn(ex.getMessage());
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
    private Role extractNiciraRoleRequest(Channel chan, OFVendor vendorMessage) {
        int vendor = vendorMessage.getVendor();
        if (vendor != OFNiciraVendorData.NX_VENDOR_ID) {
            return null;
        }
        if (!(vendorMessage.getVendorData() instanceof OFRoleRequestVendorData)) {
            return null;
        }
        OFRoleRequestVendorData roleRequestVendorData = (OFRoleRequestVendorData) vendorMessage
                .getVendorData();
        Role role = Role.fromNxRole(roleRequestVendorData.getRole());
        if (role == null) {
            String msg = String.format("Controller: [%s], State: [%s], "
                    + "received NX_ROLE_REPLY with invalid role " + "value %d",
                    chan.getRemoteAddress(), this.toString(),
                    roleRequestVendorData.getRole());
            throw new ControllerStateException(msg);
        }
        return role;
    }

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
                channel.getRemoteAddress(), m, role);
        OFMessage e = OVXMessageUtil.makeErrorMsg(
                OFBadRequestCode.OFPBRC_EPERM, m);
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
        OFVendor vendor = new OFVendor();
        vendor.setXid(xid);
        vendor.setVendor(OFNiciraVendorData.NX_VENDOR_ID);
        OFRoleReplyVendorData reply = new OFRoleReplyVendorData(role.toNxRole());
        vendor.setVendorData(reply);
        vendor.setLengthU(OFVendor.MINIMUM_LENGTH + reply.getLength());
        channel.write(Collections.singletonList(vendor));
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
    public abstract int translate(OFMessage msg, OVXPort inPort);

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
    public abstract void sendSouth(OFMessage msg, OVXPort inPort);

}
