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
package net.onrc.openvirtex.elements.port;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus.OFPortReason;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;
import net.onrc.openvirtex.messages.OVXPortStatus;
import net.onrc.openvirtex.elements.Persistable;

public class OVXPort extends Port<OVXSwitch, OVXLink> implements Persistable {

    private static Logger log = LogManager.getLogger(OVXPort.class.getName());

    private final Integer tenantId;
    private final PhysicalPort physicalPort;
    private boolean isActive;

    public OVXPort(final int tenantId, final PhysicalPort port,
            final boolean isEdge, final short portNumber)
                    throws IndexOutOfBoundException {
        super(port);
        this.tenantId = tenantId;
        this.physicalPort = port;
        try {
            this.parentSwitch = OVXMap.getInstance().getVirtualSwitch(
                    port.getParentSwitch(), tenantId);
        } catch (SwitchMappingException e) {
            // something pretty wrong if we get here. Not 100% on how to handle
            // this
            throw new RuntimeException("Unexpected state in OVXMap: "
                    + e.getMessage());
        }
        this.portNumber = portNumber;
        this.name = "ovxport-" + this.portNumber;
        this.isEdge = isEdge;
        this.hardwareAddress = port.getHardwareAddress();
        PortFeatures features = new PortFeatures();
        features.setCurrentOVXPortFeatures();
        this.currentFeatures = features.getOVXFeatures();
        features.setAdvertisedOVXPortFeatures();
        this.advertisedFeatures = features.getOVXFeatures();
        features.setSupportedOVXPortFeatures();
        this.supportedFeatures = features.getOVXFeatures();
        features.setPeerOVXPortFeatures();
        this.peerFeatures = features.getOVXFeatures();
        this.state = OFPortState.OFPPS_LINK_DOWN.getValue();
        this.config = OFPortConfig.OFPPC_NO_STP.getValue();
        this.isActive = false;

    }

    public OVXPort(final int tenantId, final PhysicalPort port,
            final boolean isEdge) throws IndexOutOfBoundException {
        this(tenantId, port, isEdge, (short) 0);
        this.portNumber = this.parentSwitch.getNextPortNumber();
        this.name = "ovxport-" + this.portNumber;
    }

    public Integer getTenantId() {
        return this.tenantId;
    }

    public PhysicalPort getPhysicalPort() {
        return this.physicalPort;
    }

    public Short getPhysicalPortNumber() {
        return this.physicalPort.getPortNumber();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isLink() {
        return !this.isEdge;
    }

    public void sendStatusMsg(OFPortReason reason) {
        OFPortStatus status = new OFPortStatus();
        status.setDesc(this);
        status.setReason(reason.getReasonCode());
        this.parentSwitch.sendMsg(status, this.parentSwitch);
    }

    /**
     * Registers a port in the virtual parent switch and in the physical port.
     */
    public void register() {
        this.parentSwitch.addPort(this);
        this.physicalPort.setOVXPort(this);
        if (this.parentSwitch.isActive()) {
            sendStatusMsg(OFPortReason.OFPPR_ADD);
            this.parentSwitch.generateFeaturesReply();
        }
        DBManager.getInstance().save(this);
    }

    /**
     * Modifies the fields of a OVXPortStatus message so that it is consistent
     * with the configs of the corresponding OVXPort.
     *
     * @param portstat the virtual port status
     */
    public void virtualizePortStat(OVXPortStatus portstat) {
        OFPhysicalPort desc = portstat.getDesc();
        desc.setPortNumber(this.portNumber);
        desc.setHardwareAddress(this.hardwareAddress);
        desc.setCurrentFeatures(this.currentFeatures);
        desc.setAdvertisedFeatures(this.advertisedFeatures);
        desc.setSupportedFeatures(this.supportedFeatures);
        portstat.setDesc(desc);
    }

    /**
     * Changes the attribute of this port according to a MODIFY PortStatus.
     *
     * @param portstat the virtual port status
     */
    public void applyPortStatus(OVXPortStatus portstat) {
        if (portstat.getReason() != OFPortReason.OFPPR_MODIFY.getReasonCode()) {
            return;
        }
        OFPhysicalPort psport = portstat.getDesc();
        this.config = psport.getConfig();
        this.state = psport.getState();
        this.peerFeatures = psport.getPeerFeatures();
    }

    public void boot() {
        if (this.isActive) {
            return;
        }
        this.isActive = true;
        this.state = OFPortState.OFPPS_STP_FORWARD.getValue();
        this.parentSwitch.generateFeaturesReply();
        if (this.parentSwitch.isActive()) {
            sendStatusMsg(OFPortReason.OFPPR_MODIFY);
        }
        if (this.isLink()) {
            this.getLink().getOutLink().getDstPort().boot();
        }
    }

    public void tearDown() {
        if (!this.isActive) {
            return;
        }
        this.isActive = false;
        this.state = OFPortState.OFPPS_LINK_DOWN.getValue();
        this.parentSwitch.generateFeaturesReply();
        if (this.parentSwitch.isActive()) {
            sendStatusMsg(OFPortReason.OFPPR_MODIFY);
        }
        if (this.isLink()) {
            this.getLink().getOutLink().getDstPort().tearDown();
        }

        cleanUpFlowMods();
    }

    public void unregister() {
        DBManager.getInstance().remove(this);
        OVXNetwork virtualNetwork = null;
        try {
            virtualNetwork = this.parentSwitch.getMap().getVirtualNetwork(
                    this.tenantId);
        } catch (NetworkMappingException e) {
            log.error(
                    "Error retrieving the network with id {}. Unregister for OVXPort {}/{} not fully done!",
                    this.getTenantId(), this.getParentSwitch().getSwitchName(),
                    this.getPortNumber());
            return;
        }
        if (this.parentSwitch.isActive()) {
            sendStatusMsg(OFPortReason.OFPPR_DELETE);
        }
        if (this.isEdge && this.isActive) {
            Host host = virtualNetwork.getHost(this);
            host.unregister();
        } else if (!this.isEdge) {
            this.getLink().egressLink.unregister();
            this.getLink().ingressLink.unregister();
        }
        this.unMap();
        this.parentSwitch.generateFeaturesReply();
        cleanUpFlowMods();
    }

    @Override
    public Map<String, Object> getDBIndex() {
        Map<String, Object> index = new HashMap<String, Object>();
        index.put(TenantHandler.TENANT, this.tenantId);
        return index;
    }

    @Override
    public String getDBKey() {
        return Port.DB_KEY;
    }

    @Override
    public String getDBName() {
        return DBManager.DB_VNET;
    }

    @Override
    public Map<String, Object> getDBObject() {
        Map<String, Object> dbObject = new HashMap<String, Object>();
        dbObject.putAll(this.getPhysicalPort().getDBObject());
        dbObject.put(TenantHandler.VPORT, this.portNumber);
        return dbObject;
    }

    private void cleanUpFlowMods() {
        log.info("Cleaning up flowmods for sw {} port {}", this
                .getPhysicalPort().getParentSwitch().getSwitchName(),
                this.getPhysicalPortNumber());
        this.getPhysicalPort().parentSwitch.cleanUpTenant(this.tenantId,
                this.getPhysicalPortNumber());
    }

    public boolean equals(final OVXPort port) {
        return this.portNumber == port.portNumber
                && this.parentSwitch.getSwitchId() == port.getParentSwitch()
                .getSwitchId();
    }

    /**
     * Undoes mapping for this port from the OVXSwitch and PhysicalPort.
     */
    public void unMap() {
        this.parentSwitch.removePort(this.portNumber);
        this.physicalPort.removeOVXPort(this);
    }

    /**
     * Removes a host from this port, if it's an edge.
     *
     * @throws NetworkMappingException
     */
    public void unMapHost() throws NetworkMappingException {
        if (this.isEdge) {
            OVXNetwork virtualNetwork = this.parentSwitch.getMap()
                    .getVirtualNetwork(this.tenantId);
            Host host = virtualNetwork.getHost(this);
            /*
             * need this check since a port can be created but not have anything
             * attached to it
             */
            if (host != null) {
                host.unregister();
            }
        }
    }

    /**
     * Deletes this port after removing any links mapped to this port.
     *
     * TODO see if this can be consolidated with unregister(), because it shares
     * a lot in common
     *
     * @param stat
     *            PortStatus triggering port deletion
     * @throws NetworkMappingException
     * @throws LinkMappingException
     */
    public void handlePortDelete(OVXPortStatus stat)
            throws NetworkMappingException {
        log.debug("deleting port {}", this.getPortNumber());
        handlePortDisable(stat);
        this.unregister();
    }

    /**
     * Checks if this port has associated OVXLink(s) and/or SwitchRoute(s) and
     * attempts to neatly disable them. This port and its neighbor are NOT
     * deleted. Since this port is an end point, OVXLink/SwitchRoute, there is
     * no real backup to recover to in this case, so we don't try.
     *
     * @param stat
     *            PortStatus triggering link down
     * @throws NetworkMappingException
     */
    public void handlePortDisable(OVXPortStatus stat)
            throws NetworkMappingException {
        handleLinkDisable(stat);
        handleRouteDisable(stat);
        this.tearDown();
        log.info("Sending " + stat.toString() + " as OVXSwitch "
                + this.parentSwitch.getSwitchId());
    }

    /**
     * Disables a link for LINK_DOWN or DELETE PortStats. Mapping s for the
     * OVXLink are removed only if the provided PortStat is of reason DELETE.
     *
     * @param stat the port status
     * @throws NetworkMappingException
     */
    public void handleLinkDisable(OVXPortStatus stat)
            throws NetworkMappingException {
        OVXNetwork virtualNetwork = this.parentSwitch.getMap()
                .getVirtualNetwork(this.tenantId);
        if (virtualNetwork.getHost(this) == null && this.portLink != null &&
                this.portLink.exists()) {
            OVXPort dst = this.portLink.egressLink.getDstPort();
            /* unmap vLinks and this port if DELETE */
            if (stat.isReason(OFPortReason.OFPPR_DELETE)) {
                this.portLink.egressLink.unregister();
                this.portLink.ingressLink.unregister();
            }
            /*
             * set this and destPort as edge, and send up Modify PortStat for
             * dest port
             */
            dst.tearDown();
        }

    }

    /**
     * Removes SwitchRoutes from a BVS's routing table if the end points of the
     * route are deleted.
     *
     * @param stat
     */
    public void handleRouteDisable(OVXPortStatus stat) {
        if ((this.parentSwitch instanceof OVXBigSwitch)
                && (stat.isReason(OFPortReason.OFPPR_DELETE))) {
            Map<OVXPort, SwitchRoute> routes = ((OVXBigSwitch) this.parentSwitch)
                    .getRouteMap().get(this);
            if (routes != null) {
                Set<SwitchRoute> rtset = Collections
                        .unmodifiableSet((Set<SwitchRoute>) routes.values());
                for (SwitchRoute route : rtset) {
                    ((OVXBigSwitch) this.parentSwitch).unregisterRoute(route
                            .getRouteId());
                }
            }
            // TODO send flowRemoved's
        }
    }

    /**
     * Brings a disabled port and its links (by association up). Currently it's
     * only the matter of setting the endpoints to nonEdge if they used to be
     * part of a link.
     *
     * @param stat
     *            PortStatus indicating link up
     * @throws NetworkMappingException
     */
    public void handlePortEnable(OVXPortStatus stat)
            throws NetworkMappingException {
        log.debug("enabling port {}", this.getPortNumber());
        OVXNetwork virtualNetwork = this.parentSwitch.getMap()
                .getVirtualNetwork(this.tenantId);
        Host h = virtualNetwork.getHost(this);
        this.boot();
        if (h != null) {
            h.getPort().boot();
        } else if (this.portLink != null && this.portLink.exists()) {
            OVXPort dst = this.portLink.egressLink.getDstPort();
            dst.boot();
            dst.isEdge = false;
            this.isEdge = false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((physicalPort == null) ? 0 : physicalPort.hashCode());
        result = prime * result
                + ((tenantId == null) ? 0 : tenantId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OVXPort)) {
            return false;
        }
        OVXPort other = (OVXPort) obj;
        if (physicalPort == null) {
            if (other.physicalPort != null) {
                return false;
            }
        } else if (!physicalPort.equals(other.physicalPort)) {
            return false;
        }
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        } else if (!tenantId.equals(other.tenantId)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        int linkId = 0;
        if (isLink()) {
            linkId = this.getLink().getOutLink().getLinkId();
        }
        return "PORT:\n- portNumber: " + this.portNumber + "\n- parentSwitch: "
        + this.getParentSwitch().getSwitchName()
        + "\n- virtualNetwork: " + this.getTenantId()
        + "\n- hardwareAddress: "
        + MACAddress.valueOf(this.hardwareAddress).toString()
        + "\n- config: " + this.config + "\n- state: " + this.state
        + "\n- currentFeatures: " + this.currentFeatures
        + "\n- advertisedFeatures: " + this.advertisedFeatures
        + "\n- supportedFeatures: " + this.supportedFeatures
        + "\n- peerFeatures: " + this.peerFeatures + "\n- isEdge: "
        + this.isEdge + "\n- isActive: " + this.isActive
        + "\n- linkId: " + linkId + "\n- physicalPortNumber: "
        + this.getPhysicalPortNumber() + "\n- physicalSwitchName: "
        + this.getPhysicalPort().getParentSwitch().getSwitchName();
    }
}
