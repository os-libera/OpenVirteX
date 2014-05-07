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
import java.util.concurrent.ConcurrentHashMap;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.DPIDandPortPair;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.linkdiscovery.SwitchDiscoveryManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;

/**
 *
 * Singleton class for physical network. Maintains SwitchDiscoveryManager for
 * each switch in the physical network. Listens for LLDP packets and passes them
 * on to the appropriate SwitchDiscoveryManager. Creates and maintains links
 * after discovery, and switch ports are made discoverable here.
 *
 * TODO: should probably subscribe to PORT UP/DOWN events here
 *
 */
public final class PhysicalNetwork extends
        Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

    private static PhysicalNetwork instance;
    private ArrayList<Uplink> uplinkList;
    private final ConcurrentHashMap<Long, SwitchDiscoveryManager> discoveryManager;
    private static HashedWheelTimer timer;
    private static Logger log = LogManager.getLogger(PhysicalNetwork.class.getName());

    private PhysicalNetwork() {
        PhysicalNetwork.log.info("Starting network discovery...");
        // PhysicalNetwork.timer = new HashedWheelTimer();
        this.discoveryManager = new ConcurrentHashMap<Long, SwitchDiscoveryManager>();
    }

    public static PhysicalNetwork getInstance() {
        if (PhysicalNetwork.instance == null) {
            PhysicalNetwork.instance = new PhysicalNetwork();
        }
        return PhysicalNetwork.instance;
    }

    public static HashedWheelTimer getTimer() {
        if (PhysicalNetwork.timer == null) {
            PhysicalNetwork.timer = new HashedWheelTimer();
        }
        return PhysicalNetwork.timer;
    }

    public static void reset() {
        log.debug("PhysicalNetwork has been explicitly reset. "
                + "Hope you know what you are doing!!");
        PhysicalNetwork.instance = null;
    }

    public ArrayList<Uplink> getUplinkList() {
        return this.uplinkList;
    }

    public void setUplinkList(final ArrayList<Uplink> uplinkList) {
        this.uplinkList = uplinkList;
    }

    /**
     * Add physical switch to topology and make it discoverable.
     *
     * @param sw the switch
     */
    @Override
    public synchronized void addSwitch(final PhysicalSwitch sw) {
        super.addSwitch(sw);
        this.discoveryManager.put(sw.getSwitchId(), new SwitchDiscoveryManager(
                sw, OpenVirteXController.getInstance().getUseBDDP()));
        DBManager.getInstance().addSwitch(sw.getSwitchId());
    }

    /**
     * Removes switch from topology discovery and mappings for this network.
     *
     * @param sw the switch
     */
    public boolean removeSwitch(final PhysicalSwitch sw) {
        DBManager.getInstance().delSwitch(sw.getSwitchId());
        SwitchDiscoveryManager sdm = this.discoveryManager
                .get(sw.getSwitchId());
        for (PhysicalPort port : sw.getPorts().values()) {
            removePort(sdm, port);
        }
        if (sdm != null) {
            this.discoveryManager.remove(sw.getSwitchId());
        }
        return super.removeSwitch(sw);
    }

    /**
     * Adds port for discovery.
     *
     * @param port the port
     */
    public synchronized void addPort(final PhysicalPort port) {
        SwitchDiscoveryManager sdm = this.discoveryManager.get(port
                .getParentSwitch().getSwitchId());
        if (sdm != null) {
            // Do not run discovery on local OpenFlow port
            if (port.getPortNumber() != OFPort.OFPP_LOCAL.getValue()) {
                sdm.addPort(port);
            }
        }
        DBManager.getInstance().addPort(port.toDPIDandPort());
    }

    /**
     * Removes port from discovery.
     *
     * @param sdm switch discovery manager
     * @param port the port
     */
    public synchronized void removePort(SwitchDiscoveryManager sdm,
            final PhysicalPort port) {
        DBManager.getInstance().delPort(port.toDPIDandPort());
        port.unregister();
        /* remove from topology discovery */
        if (sdm != null) {
            log.info("removing port {}", port.getPortNumber());
            sdm.removePort(port);
        }
        /* remove from this network's mappings */
        PhysicalPort dst = this.neighborPortMap.get(port);
        if (dst != null) {
            this.removeLink(port, dst);
        }
    }

    /**
     * Creates link and adds it to the topology.
     *
     * @param srcPort source port
     * @param dstPort destination port
     */
    public synchronized void createLink(final PhysicalPort srcPort,
            final PhysicalPort dstPort) {
        final PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
        if (neighbourPort == null || !neighbourPort.equals(dstPort)) {
            final PhysicalLink link = new PhysicalLink(srcPort, dstPort);
            OVXMap.getInstance().knownLink(link);
            super.addLink(link);
            log.info("Adding physical link between {}/{} and {}/{}", link
                    .getSrcSwitch().getSwitchName(), link.getSrcPort()
                    .getPortNumber(), link.getDstSwitch().getSwitchName(), link
                    .getDstPort().getPortNumber());
            DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcPort
                    .getParentSwitch().getSwitchId(), srcPort.getPortNumber()),
                    new DPIDandPort(dstPort.getParentSwitch().getSwitchId(),
                            dstPort.getPortNumber()));
            DBManager.getInstance().addLink(dpp);
        } else {
            log.debug("Tried to create invalid link");
        }
    }

    /**
     * Removes link from the topology.
     *
     * @param srcPort source port
     * @param dstPort destination port
     */
    public synchronized void removeLink(final PhysicalPort srcPort,
            final PhysicalPort dstPort) {
        PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
        if ((neighbourPort != null) && (neighbourPort.equals(dstPort))) {
            final PhysicalLink link = super.getLink(srcPort, dstPort);
            DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcPort
                    .getParentSwitch().getSwitchId(), srcPort.getPortNumber()),
                    new DPIDandPort(dstPort.getParentSwitch().getSwitchId(),
                            dstPort.getPortNumber()));
            DBManager.getInstance().delLink(dpp);
            super.removeLink(link);
            log.info("Removing physical link between {}/{} and {}/{}", link
                    .getSrcSwitch().getSwitchName(), link.getSrcPort()
                    .getPortNumber(), link.getDstSwitch().getSwitchName(), link
                    .getDstPort().getPortNumber());
            super.removeLink(link);
        } else {
            PhysicalNetwork.log.debug("Tried to remove invalid link");
        }
    }

    /**
     * Acknowledges receipt of discovery probe to sender port.
     *
     * @param port the port
     */
    public void ackProbe(final PhysicalPort port) {
        final SwitchDiscoveryManager sdm = this.discoveryManager.get(port
                .getParentSwitch().getSwitchId());
        if (sdm != null) {
            sdm.ackProbe(port);
        }
    }

    /**
     * Handles LLDP packets by passing them on to the appropriate
     * SwitchDisoveryManager (which sent the original LLDP packet).
     *
     * @param msg the LLDP packet in
     * @param the switch
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
        // Pass msg to appropriate SwitchDiscoveryManager
        final SwitchDiscoveryManager sdm = this.discoveryManager.get(sw
                .getSwitchId());
        if (sdm != null) {
            sdm.handleLLDP(msg, sw);
        }
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        // Do nothing
    }

    @Override
    public String getName() {
        return "Physical network";
    }

    @Override
    public boolean boot() {
        return true;
    }

    /**
     * Gets the discovery manager for the given switch.
     * TODO use MappingException to deal with null SDMs
     *
     * @param dpid the datapath ID
     * @return the discovery manager instance
     */
    public SwitchDiscoveryManager getDiscoveryManager(long dpid) {
        return this.discoveryManager.get(dpid);
    }

}
