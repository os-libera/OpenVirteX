/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.statistics.StatisticsManager;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.Virtualizable;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsReply;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.statistics.OFStatistics;

/**
 * The Class PhysicalSwitch.
 */
public class PhysicalSwitch extends Switch<PhysicalPort> {

    enum SwitchState {
        INIT {
            @Override
            public void register(final PhysicalSwitch psw) {
                psw.log.debug(
                        "Switch connected with dpid {}, name {} and type {}",
                        psw.getSwitchId(), psw.getSwitchName(),
                        psw.desc.getHardwareDescription());
                PhysicalNetwork.getInstance().addSwitch(psw);
                psw.state = SwitchState.INACTIVE;
            }
        },
        INACTIVE {
            @Override
            public boolean boot(final PhysicalSwitch psw) {
                psw.state = SwitchState.ACTIVE;
                psw.fillPortMap();
                psw.statsMan.start();
                return true;
            }

            @Override
            public void unregister(final PhysicalSwitch psw) {
                psw.deregOVXSwitch(psw);
                /* try to remove from network and disconnect */
                PhysicalNetwork.getInstance().removeSwitch(psw);
                final Collection<PhysicalPort> ports = psw.portMap.values();
                for (final PhysicalPort p : ports) {
                    p.unregister();
                }
                psw.portMap.clear();
                OVXMap.getInstance().removePhysicalSwitch(psw);
                /*
                 * not safe to completely stop a PhySwitch unless we really know
                 * for sure that it's gone for good.
                 */
                // psw.state = SwitchState.STOPPED;
            }

        },
        ACTIVE {
            @Override
            public boolean teardown(final PhysicalSwitch psw) {
                psw.statsMan.stop();
                psw.channel.disconnect();
                psw.setConnected(false);
                for (final PhysicalPort p : psw.portMap.values()) {
                    p.tearDown();
                }
                psw.log.info("Switch {} disconnected ",
                        psw.featuresReply.getDatapathId());
                psw.state = SwitchState.INACTIVE;
                return true;
            }

            @Override
            public int translate(final PhysicalSwitch psw, final OFMessage ofm,
                    final OVXSwitch sw) {
                return psw.translator.translate(ofm.getXid(), sw);
            }

            @Override
            public XidPair<OVXSwitch> untranslate(final PhysicalSwitch psw,
                    final OFMessage ofm) {
                final XidPair<OVXSwitch> pair = psw.translator.untranslate(ofm
                        .getXid());
                if (pair == null) {
                    return null;
                }
                return pair;
            }

            @Override
            public boolean addPort(final PhysicalSwitch psw,
                    final PhysicalPort port) {
                return psw.addIface(port);
            }

            @Override
            public void sendMsg(final PhysicalSwitch psw, final OFMessage msg,
                    final OVXSendMsg from) {
                if (psw.channel.isOpen() && psw.isConnected) {
                    psw.log.debug("Sending packet to sw {}: {}", psw.getName(),
                            msg);
                    psw.channel.write(Collections.singletonList(msg));
                }
            }

            @Override
            public void handleIO(final PhysicalSwitch psw, final OFMessage msg,
                    final Channel ch) {
                try {
                    ((Virtualizable) msg).virtualize(psw);
                } catch (final ClassCastException e) {
                    psw.log.error("Received illegal message : " + msg);
                }
            }

            @Override
            public boolean removePort(final PhysicalSwitch psw,
                    final PhysicalPort port) {
                return psw.removeIface(port);
            }
        },
        STOPPED {
            @Override
            public boolean boot(final PhysicalSwitch psw) {
                psw.log.warn(
                        "Switch {} has already been halted, can't re-enable",
                        psw.getSwitchName());
                return false;
            }
        };

        public static final int INVALID_XID = -1;

        public void register(final PhysicalSwitch psw) {
            psw.log.warn("Switch {} has already been registered",
                    psw.getSwitchName());
        }

        public boolean boot(final PhysicalSwitch psw) {
            psw.log.warn("Switch {} has already been enabled",
                    psw.getSwitchName());
            return false;
        }

        public boolean teardown(final PhysicalSwitch psw) {
            psw.log.warn("Switch {} has already been disabled",
                    psw.getSwitchName());
            return false;
        }

        public boolean addPort(final PhysicalSwitch psw, final PhysicalPort port) {
            psw.log.warn("Can't add port {} to Switch {} in state={}",
                    port.getPortNumber(), psw.getSwitchName(), psw.state);
            return false;
        }

        public void sendMsg(final PhysicalSwitch psw, final OFMessage msg,
                final OVXSendMsg from) {
            psw.log.warn("Switch {} can't send message while state={}",
                    psw.getSwitchName(), psw.state);
        }

        public void handleIO(final PhysicalSwitch psw, final OFMessage msg,
                final Channel ch) {
            psw.log.warn("Switch {} can't handle message while state={}",
                    psw.getSwitchName(), psw.state);
        }

        // do we want to clear out tables of stored messages that a switch
        // receives
        // while inactive? being able to remove things would prevent potential
        // aliasing.

        public int translate(final PhysicalSwitch psw, final OFMessage ofm,
                final OVXSwitch sw) {
            psw.log.warn("XIDTranslator for Switch {} inactive when state={}",
                    psw.getSwitchName(), psw.state);
            return SwitchState.INVALID_XID;
        }

        public XidPair<OVXSwitch> untranslate(final PhysicalSwitch psw,
                final OFMessage ofm) {
            psw.log.warn("XIDTranslator for Switch {} inactive when state={}",
                    psw.getSwitchName(), psw.state);
            return null;
        }

        public void unregister(final PhysicalSwitch psw) {
            psw.log.warn("Switch {} can't shut down from state={}", psw.state);
        }

        public boolean removePort(final PhysicalSwitch psw,
                final PhysicalPort port) {
            psw.log.warn("Can't remove port {} from Switch {} in state={}",
                    port.getPortNumber(), psw.getSwitchName(), psw.state);
            return false;
        }
    }

    /** The log. */
    Logger                                                                    log      = LogManager
                                                                                               .getLogger(PhysicalSwitch.class
                                                                                                       .getName());

    /** The Xid mapper */
    private final XidTranslator<OVXSwitch>                                    translator;

    private StatisticsManager                                                 statsMan = null;
    private final AtomicReference<Map<Short, OVXPortStatisticsReply>>         portStats;
    private final AtomicReference<Map<Integer, List<OVXFlowStatisticsReply>>> flowStats;
    private SwitchState                                                       state;

    /**
     * Unregisters OVXSwitches and associated virtual elements mapped to a
     * PhysicalSwitch. Called by unregister() when the PhysicalSwitch is torn
     * down.
     * 
     * Bit iffy - currently, no reliable way to make sure a PhySwitch is gone
     * for good - that means we might destroy a VSwitch before the physical one
     * is really gone.
     */
    class SwitchDeregAction implements Runnable {

        PhysicalSwitch psw;
        int            tid;

        SwitchDeregAction(final PhysicalSwitch s, final int t) {
            this.psw = s;
            this.tid = t;
        }

        @Override
        public void run() {
            OVXSwitch vsw;
            try {
                if (PhysicalSwitch.this.map
                        .hasVirtualSwitch(this.psw, this.tid)) {
                    vsw = PhysicalSwitch.this.map.getVirtualSwitch(this.psw,
                            this.tid);
                    PhysicalSwitch.this.log.info(
                            "Unregistering OVXSwitch {} [mapped to {}]",
                            vsw.getSwitchName(), this.psw.getSwitchName());

                    /* save = don't destroy the switch, it can be saved */
                    boolean save = false;
                    if (vsw instanceof OVXBigSwitch) {
                        save = ((OVXBigSwitch) vsw).tryRecovery(this.psw);
                    }
                    if (!save) {
                        this.psw.cleanUpTenant(this.tid, (short) 0);
                        vsw.tearDown(true);
                        vsw.unregister(true);
                    }
                }
            } catch (final SwitchMappingException e) {
                PhysicalSwitch.this.log.warn("Inconsistency in OVXMap: {}",
                        e.getMessage());
            }
        }
    }

    /**
     * Instantiates a new physical switch.
     *
     * @param switchId
     *            the switch id
     */
    public PhysicalSwitch(final long switchId) {
        super(switchId);
        this.translator = new XidTranslator<OVXSwitch>();
        this.portStats = new AtomicReference<Map<Short, OVXPortStatisticsReply>>();
        this.flowStats = new AtomicReference<Map<Integer, List<OVXFlowStatisticsReply>>>();
        this.statsMan = new StatisticsManager(this);
        this.state = SwitchState.INIT;
    }

    /**
     * Gets the OVX port number.
     *
     * @param physicalPortNumber
     *            the physical port number
     * @param tenantId
     *            the tenant id
     * @param vLinkId
     *            the virtual link ID
     * @return the virtual port number
     */
    public Short getOVXPortNumber(final Short physicalPortNumber,
            final Integer tenantId, final Integer vLinkId) {
        return this.portMap.get(physicalPortNumber)
                .getOVXPort(tenantId, vLinkId).getPortNumber();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
     */
    @Override
    public boolean tearDown() {
        return this.state.teardown(this);
    }

    /**
     * Fill port map. Assume all ports are edges until discovery says otherwise.
     */
    protected void fillPortMap() {
        for (final OFPhysicalPort port : this.featuresReply.getPorts()) {
            final PhysicalPort physicalPort = new PhysicalPort(port, this, true);
            physicalPort.register();
        }
    }

    @Override
    public boolean addPort(final PhysicalPort port) {
        return this.state.addPort(this, port);
    }

    /**
     * Adds a port to this PhysicalSwitch ONLY if this switch and the
     * PhysicalNetwork are ACTIVE.
     * 
     * @param port
     *            the PhysicalPort to add
     * @return true if successfully added.
     */
    private boolean addIface(final PhysicalPort port) {
        final boolean result = super.addPort(port);
        if (result) {
            PhysicalNetwork.getInstance().addPort(port);
        }
        return result;
    }

    /**
     * Removes the specified port from this PhysicalSwitch. This includes
     * removal from the switch's port map, topology discovery, and the
     * PhysicalNetwork topology.
     *
     * @param port
     *            the physical port instance
     * @return true if successful, false otherwise
     */
    public boolean removePort(final PhysicalPort port) {
        return this.state.removePort(this, port);
    }

    private boolean removeIface(final PhysicalPort port) {
        return super.removePort(port.getPortNumber());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#init()
     */
    @Override
    public boolean boot() {
        return this.state.boot(this);
    }

    @Override
    public void register() {
        this.state.register(this);
    }

    /**
     * Removes this PhysicalSwitch from the network. Also removes associated
     * ports, links, and virtual elements mapped to it (OVX*Switch, etc.).
     */
    @Override
    public void unregister() {
        this.state.unregister(this);
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        this.state.sendMsg(this, msg, from);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
     */
    @Override
    public String toString() {
        return "DPID : "
                + this.switchId
                + ", remoteAddr : "
                + (this.channel == null ? "None" : this.channel
                        .getRemoteAddress().toString());
    }

    /**
     * Gets the port.
     *
     * @param portNumber
     *            the port number
     * @return the port instance
     */
    @Override
    public PhysicalPort getPort(final Short portNumber) {
        return this.portMap.get(portNumber);
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof PhysicalSwitch) {
            return this.switchId == ((PhysicalSwitch) other).switchId;
        }

        return false;
    }

    public int translate(final OFMessage ofm, final OVXSwitch sw) {
        return this.state.translate(this, ofm, sw);
    }

    public XidPair<OVXSwitch> untranslate(final OFMessage ofm) {
        return this.state.untranslate(this, ofm);
    }

    public void setPortStatistics(final Map<Short, OVXPortStatisticsReply> stats) {
        this.portStats.set(stats);
    }

    public void setFlowStatistics(
            final Map<Integer, List<OVXFlowStatisticsReply>> stats) {
        this.flowStats.set(stats);

    }

    public List<OVXFlowStatisticsReply> getFlowStats(final int tid) {
        final Map<Integer, List<OVXFlowStatisticsReply>> stats = this.flowStats
                .get();
        if (stats != null && stats.containsKey(tid)) {
            return Collections.unmodifiableList(stats.get(tid));
        }
        return null;
    }

    public OVXPortStatisticsReply getPortStat(final short portNumber) {
        final Map<Short, OVXPortStatisticsReply> stats = this.portStats.get();
        if (stats != null) {
            return stats.get(portNumber);
        }
        return null;
    }

    public void cleanUpTenant(final Integer tenantId, final Short port) {
        this.statsMan.cleanUpTenant(tenantId, port);
    }

    public void removeFlowMods(final OVXStatisticsReply msg) {
        final int tid = msg.getXid() >> 16;
        final short port = (short) (msg.getXid() & 0xFFFF);
        for (final OFStatistics stat : msg.getStatistics()) {
            final OVXFlowStatisticsReply reply = (OVXFlowStatisticsReply) stat;
            if (tid != this.getTidFromCookie(reply.getCookie())) {
                continue;
            }
            if (port != 0) {
                this.sendDeleteFlowMod(reply, port);
                if (reply.getMatch().getInputPort() == port) {
                    this.sendDeleteFlowMod(reply, OFPort.OFPP_NONE.getValue());
                }
            } else {
                this.sendDeleteFlowMod(reply, OFPort.OFPP_NONE.getValue());
            }
        }
    }

    private void sendDeleteFlowMod(final OVXFlowStatisticsReply reply,
            final short port) {
        final OVXFlowMod dFm = new OVXFlowMod();
        dFm.setCommand(OFFlowMod.OFPFC_DELETE_STRICT);
        dFm.setMatch(reply.getMatch());
        dFm.setOutPort(port);
        dFm.setLengthU(OFFlowMod.MINIMUM_LENGTH);
        this.sendMsg(dFm, this);
    }

    private int getTidFromCookie(final long cookie) {
        return (int) (cookie >> 32);
    }

    /**
     * Unregisters OVXSwitches mapped to provided PhysicalSwitch
     */
    public void deregOVXSwitch(final PhysicalSwitch psw) {
        for (final Integer tid : this.map.listVirtualNetworks().keySet()) {
            final SwitchDeregAction dereg = new SwitchDeregAction(psw, tid);
            new Thread(dereg).start();
        }
    }

    @Override
    public void handleRoleIO(final OFVendor msg, final Channel channel) {
        this.log.warn(
                "Received Role message {} from switch {}, but no role was requested",
                msg, this.switchName);
    }

    @Override
    public void removeChannel(final Channel channel) {
    }

    @Override
    public boolean isActive() {
        return this.state.equals(SwitchState.ACTIVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(final OFMessage msg, final Channel channel) {
        this.state.handleIO(this, msg, channel);
    }
}
