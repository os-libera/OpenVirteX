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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.statistics.StatisticsManager;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.Virtualizable;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatsReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;

public class PhysicalSwitch extends Switch<PhysicalPort> {
    private static Logger log = LogManager.getLogger(PhysicalSwitch.class.getName());

        // The Xid mapper
    private final XidTranslator<OVXSwitch> translator;
    private StatisticsManager statsMan = null;

    private AtomicReference<Map<Short, OFPortStatsEntry>> portStats;

    //private AtomicReference<Map<Short, OVXPortStatisticsReply>> portStats;
    //private AtomicReference<Map<Integer, List<OVXFlowStatisticsReply>>> flowStats;

    private AtomicReference<Map<Integer, List<OFFlowStatsEntry>>> flowStats;

    class DeregAction implements Runnable {

        private PhysicalSwitch psw;
        private int tid;

        DeregAction(PhysicalSwitch s, int t) {
            this.psw = s;
            this.tid = t;
        }

        @Override
        public void run() {
            OVXSwitch vsw;
            try {
                if (psw.map.hasVirtualSwitch(psw, tid)) {
                    vsw = psw.map.getVirtualSwitch(psw, tid);
                    /* save = don't destroy the switch, it can be saved */
                    boolean save = false;
                    if (vsw instanceof OVXBigSwitch) {
                        save = ((OVXBigSwitch) vsw).tryRecovery(psw);
                    }
                    if (!save) {
                        vsw.unregister();
                    }
                }
            } catch (SwitchMappingException e) {
                log.warn("Inconsistency in OVXMap: {}", e.getMessage());
            }
        }
    }

    /**
     * Instantiates a new physical switch.
     *
     * @param switchId
     *            the switch id
     */
    public PhysicalSwitch(final long switchId, OFVersion ofv) {
        super(switchId);
        this.translator = new XidTranslator<OVXSwitch>();
        this.portStats = new AtomicReference<Map<Short, OFPortStatsEntry>>();
        this.flowStats = new AtomicReference<Map<Integer, List<OFFlowStatsEntry>>>();

        this.setOfVersion(ofv);

        this.statsMan = new StatisticsManager(this);
    }

    /**
     * Gets the OVX port number.
     *
     * @param physicalPortNumber the physical port number
     * @param tenantId the tenant id
     * @param vLinkId the virtual link ID
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
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(final OVXMessage msg, Channel channel) {
        try {
            ((Virtualizable) msg).virtualize(this);
        } catch (final ClassCastException e) {
            PhysicalSwitch.log.error("Received illegal message : " + msg.getOFMessage().toString());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
     */
    @Override
    public void tearDown() {
        PhysicalSwitch.log.info("Switch disconnected {} ",
                this.featuresReply.getDatapathId());
        this.statsMan.stop();
        this.channel.disconnect();
        this.map.removePhysicalSwitch(this);
    }

    /**
     * Fill port map. Assume all ports are edges until discovery says otherwise.
     */
    protected void fillPortMap() {
        //for (final OFPortDesc port : this.featuresReply.getPorts()) {
        for (final OFPortDesc port : this.ports) {
            final PhysicalPort physicalPort = new PhysicalPort(port, this, true);
            this.addPort(physicalPort);
        }
    }

    @Override
    public boolean addPort(final PhysicalPort port) {
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
     * @param port the physical port instance
     * @return true if successful, false otherwise
     */
    public boolean removePort(final PhysicalPort port) {
        final boolean result = super.removePort(port.getPortNumber());
        if (result) {
            PhysicalNetwork pnet = PhysicalNetwork.getInstance();
            pnet.removePort(pnet.getDiscoveryManager(this.getSwitchId()), port);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.onrc.openvirtex.elements.datapath.Switch#init()
     */
    @Override
    public boolean boot() {
        PhysicalSwitch.log.info("Switch connected with dpid {}, name {} and type {}",
                this.featuresReply.getDatapathId(), this.getSwitchName(),
                this.desc.getOFMessage().getHwDesc());
        PhysicalNetwork.getInstance().addSwitch(this);
        this.fillPortMap();
        this.statsMan.start();

        //for OVS new version
        sendDefaultFlowsAdd();

        return true;
    }

    private void sendDefaultFlowsAdd() {
        HashSet<OFFlowModFlags> flags = new HashSet<OFFlowModFlags>();
        flags.add(OFFlowModFlags.SEND_FLOW_REM);

        OFAction output = ofFactory.actions().buildOutput()
                .setPort(OFPort.CONTROLLER)
                .setMaxLen(0xffff)
                .build();

        OFFlowAdd ofFlowAdd = ofFactory.buildFlowAdd()
                .setMatch(ofFactory.buildMatch()
                        .setExact(MatchField.ETH_TYPE, EthType.LLDP)
                        .build())
                .setActions(Collections.singletonList(output))
                .setPriority(40000)
                .setFlags(flags)
                .build();
        OVXMessage dFm = new OVXMessage(ofFlowAdd);
        this.sendMsg(dFm, this);

        ofFlowAdd = ofFactory.buildFlowAdd()
                .setMatch(ofFactory.buildMatch()
                        .setExact(MatchField.ETH_TYPE, EthType.ARP)
                        .build())
                .setActions(Collections.singletonList(output))
                .setPriority(40000)
                .setFlags(flags)
                .build();
        dFm = new OVXMessage(ofFlowAdd);
        this.sendMsg(dFm, this);

        ofFlowAdd = ofFactory.buildFlowAdd()
                .setMatch(ofFactory.buildMatch()
                        .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                        .build())
                .setActions(Collections.singletonList(output))
                .setPriority(5)
                .setFlags(flags)
                .build();
        dFm = new OVXMessage(ofFlowAdd);
        this.sendMsg(dFm, this);

        ofFlowAdd = ofFactory.buildFlowAdd()
                .setMatch(ofFactory.buildMatch()
                        .setExact(MatchField.ETH_TYPE, EthType.ARP)
                        .build())
                .setActions(Collections.singletonList(output))
                .setPriority(5)
                .setFlags(flags)
                .build();
        dFm = new OVXMessage(ofFlowAdd);
        this.sendMsg(dFm, this);

    }

    /**
     * Removes this PhysicalSwitch from the network. Also removes associated
     * ports, links, and virtual elements mapped to it (OVX*Switch, etc.).
     */
    @Override
    public void unregister() {
        /* tear down OVXSingleSwitches mapped to this PhysialSwitch */
        for (Integer tid : this.map.listVirtualNetworks().keySet()) {
            DeregAction dereg = new DeregAction(this, tid);
            new Thread(dereg).start();
        }
        /* try to remove from network and disconnect */
        PhysicalNetwork.getInstance().removeSwitch(this);
        this.portMap.clear();
        this.tearDown();
    }

    @Override
    public void sendMsg(final OVXMessage msg, final OVXSendMsg from) {
        if ((this.channel.isOpen()) && (this.isConnected)) {
            this.channel.write(Collections.singletonList(msg.getOFMessage()));
        }
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
                + ((this.channel == null) ? "None" : this.channel
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

    public int translate(final OVXMessage ofm, final OVXSwitch sw) {
        return this.translator.translate((int)ofm.getOFMessage().getXid(), sw);
    }

    public XidPair<OVXSwitch> untranslate(final OVXMessage ofm) {
        final XidPair<OVXSwitch> pair = this.translator.untranslate((int)ofm.getOFMessage().getXid());
        if (pair == null) {
            return null;
        }
        return pair;
    }

    public void setPortStatistics(Map<Short, OFPortStatsEntry> stats) {
        this.portStats.set(stats);
    }

    public void setFlowStatistics(
            Map<Integer, List<OFFlowStatsEntry>> stats) {
        this.flowStats.set(stats);

    }

    public List<OFFlowStatsEntry> getFlowStats(int tid) {
        Map<Integer, List<OFFlowStatsEntry>> stats = this.flowStats.get();
        if (stats != null && stats.containsKey(tid)) {
            return Collections.unmodifiableList(stats.get(tid));
        }
        return null;
    }

    public OFPortStatsEntry getPortStat(short portNumber) {
        Map<Short, OFPortStatsEntry> stats = this.portStats.get();
        if (stats != null) {
            return stats.get(portNumber);
        }
        return null;
    }

    public void cleanUpTenant(Integer tenantId, Short port) {
        this.statsMan.cleanUpTenant(tenantId, port);
    }

    public void removeFlowMods(OVXStatisticsReply msg) {
        OFFlowStatsReply ofStatsReply = (OFFlowStatsReply)msg.getOFMessage();

        int tid = (int)ofStatsReply.getXid() >> 16;
        short port = (short) (ofStatsReply.getXid() & 0xFFFF);

        for (OFFlowStatsEntry stat : ofStatsReply.getEntries()) {
            //OVXFlowStatisticsReply2 reply = (OVXFlowStatisticsReply2) stat;

            OVXFlowStatsReply reply = new OVXFlowStatsReply(msg.getOFMessage(), stat);

            if (tid != this.getTidFromCookie(stat.getCookie().getValue())) {
                continue;
            }

            if (port != 0) {
                sendDeleteFlowMod(reply, port);
                if (stat.getMatch().get(MatchField.IN_PORT).getShortPortNumber() == port) {
                    sendDeleteFlowMod(reply, OFPort.ANY.getShortPortNumber());
                }
            } else {
                sendDeleteFlowMod(reply, OFPort.ANY.getShortPortNumber());
            }
        }
    }

    private void sendDeleteFlowMod(OVXFlowStatsReply reply, short port) {
        //log.info("sendDeleteFlowMod");
        OFFlowDeleteStrict ofFlowDeleteStrict = ofFactory.buildFlowDeleteStrict()
                .setMatch(reply.getOFFlowStatsEntry().getMatch())
                .setOutPort(OFPort.of(port))
                .build();
        OVXFlowMod dFm = new OVXFlowMod(ofFlowDeleteStrict);

        this.sendMsg(dFm, this);
    }

    private int getTidFromCookie(long cookie) {
        return (int) (cookie >> 32);
    }

    @Override
    public void handleRoleIO(OVXMessage msg, Channel channel) {
        log.warn(
                "Received Role message {} from switch {}, but no role was requested",
                msg.getOFMessage().toString(), this.switchName);
    }

    @Override
    public void removeChannel(Channel channel) {

    }
}
