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
import java.util.concurrent.TimeUnit;

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

    /**
     * The states of the PhysicalNetwork. Note, methods like boot() and
     * register() are there for procedural consistency w.r.t other components.
     */
    enum NetworkState {
        INIT {
            @Override
            protected boolean register() {
                log.info("Booting...");
                instance.state = NetworkState.INACTIVE;
                return true;
            }
        },
        INACTIVE {
            @Override
            protected boolean boot() {
                log.info("Enabling...");
                instance.state = NetworkState.ACTIVE;
                return true;
            }

            @Override
            protected void unregister() {
                log.info("Shutting down...");
                instance.state = NetworkState.STOPPED;
                PhysicalNetwork.reset();
            }
        },
        ACTIVE {
            @Override
            protected boolean teardown() {
                log.info("Disabling...");
                instance.state = NetworkState.INACTIVE;
                return true;
            }

            @Override
            protected void addSwitch(PhysicalSwitch sw) {
                log.info("adding new Switch [DPID={}]", sw.getSwitchName());
                instance.addDP(sw);
            }

            @Override
            protected boolean removeSwitch(PhysicalSwitch sw) {
                log.info("removing Switch [DPID={}]", sw.getSwitchName());
                return instance.removeDP(sw);
            }

            @Override
            protected void addPort(PhysicalPort port) {
                SwitchDiscoveryManager sdm = instance.discoveryManager.get(port
                        .getParentSwitch().getSwitchId());
                if ((sdm != null)
                        && (port.getPortNumber() != OFPort.OFPP_LOCAL
                                .getValue())) {
                    // Do not run discovery on local OpenFlow port
                    sdm.addPort(port);
                }
            }

            @Override
            protected void removePort(PhysicalPort port) {
                disablePort(port);
                /* remove link from this network's mappings */
                PhysicalPort dst = instance.neighborPortMap.get(port);
                if (dst != null) {
                    this.removeLink(port, dst);
                }
            }

            @Override
            protected void removeLink(final PhysicalPort src,
                    final PhysicalPort dst) {
                instance.removeEdge(src, dst);
            }

            @Override
            @SuppressWarnings("rawtypes")
            protected void handleLLDP(OFMessage msg, Switch sw) {
                // Pass msg to appropriate SwitchDiscoveryManager
                final SwitchDiscoveryManager sdm = instance.discoveryManager
                        .get(sw.getSwitchId());
                if (sdm != null) {
                    sdm.handleLLDP(msg, sw);
                }
            }

            @Override
            protected void disablePort(PhysicalPort port) {
                SwitchDiscoveryManager sdm;
                sdm = instance.discoveryManager.get(port.getParentSwitch()
                        .getSwitchId());
                if (sdm == null) {
                    log.warn("Attempted to disable a non-existent PhysicalLink");
                    return;
                }
                sdm.removePort(port);
            }
        },
        STOPPED;

        protected boolean boot() {
            log.warn("Already booted");
            return false;
        }

        protected boolean register() {
            log.warn("Already registered");
            return false;
        }

        protected boolean teardown() {
            log.warn("Can't disable from state={}", instance.state);
            return false;
        }

        protected void unregister() {
            log.warn("Can't shut down from state={}, teardown first?",
                    instance.state);
        }

        @SuppressWarnings("rawtypes")
        protected void handleLLDP(OFMessage msg, Switch sw) {
            log.warn("ignoring LLDPs while state={}", instance.state);
        }

        protected void addPort(PhysicalPort port) {
            log.warn("can't add new port [{}, DP={}] while state={}", port
                    .getPortNumber(), port.getParentSwitch().getSwitchName(),
                    instance.state);
        }

        protected void removePort(PhysicalPort port) {
            log.warn("can't remove port [{}, DP={}] while state={}", port
                    .getPortNumber(), port.getParentSwitch().getSwitchName(),
                    instance.state);
        }

        protected void addSwitch(PhysicalSwitch sw) {
            log.warn("can't add new switch [DPID={}] while state={}",
                    sw.getSwitchName(), instance.state);
        }

        protected boolean removeSwitch(PhysicalSwitch sw) {
            log.warn("can't remove switch [DPID={}] while state={}",
                    sw.getSwitchName(), instance.state);
            return false;
        }

        protected void removeLink(PhysicalPort src, PhysicalPort dst) {
            log.warn(
                    "can't remove link [src={}/{}, dst={}/{}] while state={}",
                    new Object[] { src.getParentSwitch().getSwitchName(),
                            src.getPortNumber(),
                            dst.getParentSwitch().getSwitchName(),
                            dst.getPortNumber(), instance.state });
        }

        protected void disablePort(PhysicalPort port) {

        }
    }

    private static PhysicalNetwork instance;
    private ArrayList<Uplink> uplinkList;
    private final ConcurrentHashMap<Long, SwitchDiscoveryManager> discoveryManager;
    private static HashedWheelTimer timer;
    private static Logger log = LogManager.getLogger(PhysicalNetwork.class.getName());
    private NetworkState state;

    private PhysicalNetwork() {
        PhysicalNetwork.log.info("Starting network discovery...");
        // PhysicalNetwork.timer = new HashedWheelTimer();
        this.discoveryManager = new ConcurrentHashMap<Long, SwitchDiscoveryManager>();
        this.state = NetworkState.INIT;
    }

    public static PhysicalNetwork getInstance() {
        if (PhysicalNetwork.instance == null) {
            PhysicalNetwork.instance = new PhysicalNetwork();
        }
        return PhysicalNetwork.instance;
    }

    public static HashedWheelTimer getTimer() {
        if (PhysicalNetwork.timer == null) {
            /*
             * 100ms tickduration is absolutely fine for I/O timeouts.
             * If not, ask yourself "why?"
             */
            PhysicalNetwork.timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS,
                    OpenVirteXController.getInstance().getHashSize());
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
        this.state.addSwitch(sw);
    }

    /**
     * Helper method to addSwitch, invoked when state is ACTIVE.
     *
     * @param sw
     */
    private void addDP(final PhysicalSwitch sw) {
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
    @Override
    public boolean removeSwitch(final PhysicalSwitch sw) {
        return this.state.removeSwitch(sw);
    }

    private boolean removeDP(final PhysicalSwitch sw) {
        DBManager.getInstance().delSwitch(sw.getSwitchId());
        SwitchDiscoveryManager sdm = this.discoveryManager
                .get(sw.getSwitchId());
        /* only called from ACTIVE, so we can do this */
        for (PhysicalPort port : sw.getPorts().values()) {
            sdm.removePort(port);
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
        this.state.addPort(port);
    }

    /**
     * Removes port from discovery.
     *
     * @param sdm switch discovery manager
     * @param port the port
     */
    public synchronized void removePort(final PhysicalPort port) {
        this.state.removePort(port);
    }

    /**
     * Create link and add it to the topology. TODO: should only add when ports
     * are added to the NW. this preserves symmetry wherein links are deleted
     * ONLY if ports are deleted. Otherwise links should only deactivate, not be
     * ripped out of the topology.
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
            log.info("Adding physical link between {} and {}", link
                    .getSrcPort().toAP(), link.getDstPort().toAP());
            DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcPort
                    .getParentSwitch().getSwitchId(), srcPort.getPortNumber()),
                    new DPIDandPort(dstPort.getParentSwitch().getSwitchId(),
                            dstPort.getPortNumber()));
            DBManager.getInstance().addLink(dpp);
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
        this.state.removeLink(srcPort, dstPort);
    }

    /**
     * helper method called by removeLink in ACTIVE state
     */
    private synchronized void removeEdge(final PhysicalPort srcPort,
            final PhysicalPort dstPort) {
        PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
        if ((neighbourPort != null) && (neighbourPort.equals(dstPort))) {
            final PhysicalLink link = super.getLink(srcPort, dstPort);
            DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcPort
                    .getParentSwitch().getSwitchId(), srcPort.getPortNumber()),
                    new DPIDandPort(dstPort.getParentSwitch().getSwitchId(),
                            dstPort.getPortNumber()));
            DBManager.getInstance().delLink(dpp);
            link.unregister();
            super.removeLink(link); /* sets ports to edge */
            log.info("Removing physical link between {} and {}", link
                    .getSrcPort().toAP(), link.getDstPort().toAP());
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
        this.state.handleLLDP(msg, sw);
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
        return this.state.boot();
    }

    // TODO use MappingException to deal with null SDMs.
    /**
     * Gets the discovery manager for the given switch.
     * TODO use MappingException to deal with null SDMs
     *
     * @param switchDPID the datapath ID
     * @return the discovery manager instance
     */
    public SwitchDiscoveryManager getDiscoveryManager(long switchDPID) {
        return this.discoveryManager.get(switchDPID);
    }

    @Override
    public void register() {
        this.state.register();
    }

    @Override
    public void unregister() {
        this.state.unregister();
    }

    @Override
    public boolean tearDown() {
        return this.state.teardown();
    }

    /**
     * Deactivates a PhysicalLink by removing it from topology discovery.
     *
     * @param link
     */
    public void disablePort(final PhysicalPort port) {
        this.state.disablePort(port);
    }

}
