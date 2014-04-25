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
package net.onrc.openvirtex.linkdiscovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.messages.OVXMessageFactory;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.packet.OVXLLDP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every probeRate milliseconds, loop over all fast ports and send an
 * LLDP, send an LLDP for a single slow port. Based on FlowVisor topology
 * discovery implementation.
 *
 * TODO: add 'fast discovery' mode: drop LLDPs in destination switch but listen
 * for flow_removed messages
 */
public class SwitchDiscoveryManager implements LLDPEventHandler, OVXSendMsg,
        TimerTask {

    private final PhysicalSwitch sw;
    // send 1 probe every probeRate milliseconds
    private final long probeRate;
    private final Set<Short> slowPorts;
    private final Set<Short> fastPorts;
    // number of unacknowledged probes per port
    private final Map<Short, AtomicInteger> portProbeCount;
    // number of probes to send before link is removed
    private static final short MAX_PROBE_COUNT = 3;
    private Iterator<Short> slowIterator;
    private final OVXMessageFactory ovxMessageFactory = OVXMessageFactory
            .getInstance();
    private Logger log = LogManager.getLogger(SwitchDiscoveryManager.class.getName());
    private OVXLLDP lldpPacket;
    private Ethernet ethPacket;
    private Ethernet bddpEth;
    private final boolean useBDDP;

    /**
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param sw the physical switch
     * @param useBDDP flag to also use BDDP for discovery
     */
    public SwitchDiscoveryManager(final PhysicalSwitch sw, Boolean... useBDDP) {
        this.sw = sw;
        this.probeRate = 1000;
        this.slowPorts = Collections.synchronizedSet(new HashSet<Short>());
        this.fastPorts = Collections.synchronizedSet(new HashSet<Short>());
        this.portProbeCount = new HashMap<Short, AtomicInteger>();
        this.lldpPacket = new OVXLLDP();
        this.lldpPacket.setSwitch(this.sw);
        this.ethPacket = new Ethernet();
        this.ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        this.ethPacket.setDestinationMACAddress(OVXLLDP.LLDP_NICIRA);
        this.ethPacket.setPayload(this.lldpPacket);
        this.ethPacket.setPad(true);
        this.useBDDP = useBDDP.length > 0 ? useBDDP[0] : false;
        if (this.useBDDP) {
            this.bddpEth = new Ethernet();
            this.bddpEth.setPayload(this.lldpPacket);
            this.bddpEth.setEtherType(Ethernet.TYPE_BSN);
            this.bddpEth.setDestinationMACAddress(OVXLLDP.BDDP_MULTICAST);
            this.bddpEth.setPad(true);
            log.info("Using BDDP to discover network");
        }
        PhysicalNetwork.getTimer().newTimeout(this, this.probeRate,
                TimeUnit.MILLISECONDS);
        this.log.debug("Started discovery manager for switch {}",
                sw.getSwitchId());

    }

    /**
     * Add physical port port to discovery process.
     * Send out initial LLDP and label it as slow port.
     *
     * @param port the port
     */
    public void addPort(final PhysicalPort port) {
        // Ignore ports that are not on this switch
        if (port.getParentSwitch().equals(this.sw)) {
            synchronized (this) {
                this.log.debug("sending init probe to port {}",
                        port.getPortNumber());
                OFPacketOut pkt;
                try {
                    pkt = this.createLLDPPacketOut(port);
                    this.sendMsg(pkt, this);
                    if (useBDDP) {
                        OFPacketOut bpkt = this.createBDDPPacketOut(port);
                        this.sendMsg(bpkt, this);
                    }
                } catch (PortMappingException e) {
                    log.warn(e.getMessage());
                    return;
                }
                this.slowPorts.add(port.getPortNumber());
                this.slowIterator = this.slowPorts.iterator();
            }
        }
    }

    /**
     * Removes physical port from discovery process.
     *
     * @param port the port
     */
    public void removePort(final PhysicalPort port) {
        // Ignore ports that are not on this switch
        if (port.getParentSwitch().equals(this.sw)) {
            short portnum = port.getPortNumber();
            synchronized (this) {
                if (this.slowPorts.contains(portnum)) {
                    this.slowPorts.remove(portnum);
                    this.slowIterator = this.slowPorts.iterator();

                } else if (this.fastPorts.contains(portnum)) {
                    this.fastPorts.remove(portnum);
                    this.portProbeCount.remove(portnum);
                    // no iterator to update
                } else {
                    this.log.warn(
                            "tried to dynamically remove non-existing port {}",
                            portnum);
                }
            }
        }
    }

    /**
     * Method called by remote port to acknowledge receipt of LLDP sent by
     * this port. If slow port, updates label to fast. If fast port, decrements
     * number of unacknowledged probes.
     *
     * @param port the port
     */
    public void ackProbe(final PhysicalPort port) {
        if (port.getParentSwitch().equals(this.sw)) {
            final short portNumber = port.getPortNumber();
            synchronized (this) {
                if (this.slowPorts.contains(portNumber)) {
                    this.log.debug("Setting slow port to fast: {}:{}", port
                            .getParentSwitch().getSwitchId(), portNumber);
                    this.slowPorts.remove(portNumber);
                    this.slowIterator = this.slowPorts.iterator();
                    this.fastPorts.add(portNumber);
                    this.portProbeCount.put(portNumber, new AtomicInteger(0));
                } else {
                    if (this.fastPorts.contains(portNumber)) {
                        this.portProbeCount.get(portNumber).decrementAndGet();
                    } else {
                        this.log.debug(
                                "Got ackProbe for non-existing port: {}",
                                portNumber);
                    }
                }
            }
        }
    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     * @throws PortMappingException
     */
    private OFPacketOut createLLDPPacketOut(final PhysicalPort port)
            throws PortMappingException {
        if (port == null) {
            throw new PortMappingException(
                    "Cannot send LLDP associated with a nonexistent port");
        }
        final OFPacketOut packetOut = (OFPacketOut) this.ovxMessageFactory
                .getMessage(OFType.PACKET_OUT);
        packetOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        final List<OFAction> actionsList = new LinkedList<OFAction>();
        final OFActionOutput out = (OFActionOutput) this.ovxMessageFactory
                .getAction(OFActionType.OUTPUT);
        out.setPort(port.getPortNumber());
        actionsList.add(out);
        packetOut.setActions(actionsList);
        final short alen = SwitchDiscoveryManager.countActionsLen(actionsList);
        this.lldpPacket.setPort(port);
        this.ethPacket.setSourceMACAddress(port.getHardwareAddress());

        final byte[] lldp = this.ethPacket.serialize();
        packetOut.setActionsLength(alen);
        packetOut.setPacketData(lldp);
        packetOut
                .setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + lldp.length));
        return packetOut;
    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     * @throws PortMappingException
     */
    private OFPacketOut createBDDPPacketOut(final PhysicalPort port)
            throws PortMappingException {
        if (port == null) {
            throw new PortMappingException(
                    "Cannot send LLDP associated with a nonexistent port");
        }
        final OFPacketOut packetOut = (OFPacketOut) this.ovxMessageFactory
                .getMessage(OFType.PACKET_OUT);
        packetOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        final List<OFAction> actionsList = new LinkedList<OFAction>();
        final OFActionOutput out = (OFActionOutput) this.ovxMessageFactory
                .getAction(OFActionType.OUTPUT);
        out.setPort(port.getPortNumber());
        actionsList.add(out);
        packetOut.setActions(actionsList);
        final short alen = SwitchDiscoveryManager.countActionsLen(actionsList);
        this.lldpPacket.setPort(port);
        this.bddpEth.setSourceMACAddress(port.getHardwareAddress());

        final byte[] bddp = this.bddpEth.serialize();
        packetOut.setActionsLength(alen);
        packetOut.setPacketData(bddp);
        packetOut
                .setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + bddp.length));
        return packetOut;
    }


    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        this.sw.sendMsg(msg, this);
    }

    /**
     * Count the number of actions in a list of actions.
     *
     * @param actionsList list of actions
     * @return the number of actions
     */
    private static short countActionsLen(final List<OFAction> actionsList) {
        short count = 0;
        for (final OFAction act : actionsList) {
            count += act.getLength();
        }
        return count;
    }


    @Override
    public String getName() {
        return "SwitchDiscoveryManager " + this.sw.getName();
    }

    /*
     * Handles an incoming LLDP packet. Creates link in topology and sends ACK
     * to port where LLDP originated.
     */
    @SuppressWarnings("rawtypes")
    public void handleLLDP(final OFMessage msg, final Switch sw) {
        final OVXPacketIn pi = (OVXPacketIn) msg;
        final byte[] pkt = pi.getPacketData();

        if (OVXLLDP.isOVXLLDP(pkt)) {
            final PhysicalPort dstPort = (PhysicalPort) sw.getPort(pi
                    .getInPort());
            final DPIDandPort dp = OVXLLDP.parseLLDP(pkt);
            final PhysicalSwitch srcSwitch = PhysicalNetwork.getInstance()
                    .getSwitch(dp.getDpid());
            final PhysicalPort srcPort = srcSwitch.getPort(dp.getPort());

            PhysicalNetwork.getInstance().createLink(srcPort, dstPort);
            PhysicalNetwork.getInstance().ackProbe(srcPort);
        } else {
            this.log.warn("Ignoring unknown LLDP");
        }
    }

    /**
     * Execute this method every t milliseconds. Loops over all ports
     * labeled as fast and sends out an LLDP. Send out an LLDP on a single slow
     * port.
     *
     * @param t timeout
     * @throws Exception
     */
    @Override
    public void run(final Timeout t) {
        this.log.debug("sending probes");
        synchronized (this) {
            final Iterator<Short> fastIterator = this.fastPorts.iterator();
            while (fastIterator.hasNext()) {
                final Short portNumber = fastIterator.next();
                final int probeCount = this.portProbeCount.get(portNumber)
                        .getAndIncrement();
                if (probeCount < SwitchDiscoveryManager.MAX_PROBE_COUNT) {
                    this.log.debug("sending fast probe to port");
                    try {
                        OFPacketOut pkt = this.createLLDPPacketOut(this.sw
                                .getPort(portNumber));
                        this.sendMsg(pkt, this);
                        if (useBDDP) {
                            OFPacketOut bpkt = this.createBDDPPacketOut(this.sw
                                    .getPort(portNumber));
                            this.sendMsg(bpkt, this);
                        }
                    } catch (PortMappingException e) {
                        log.warn(e.getMessage());
                    }
                } else {
                    // Update fast and slow ports
                    fastIterator.remove();
                    this.slowPorts.add(portNumber);
                    this.slowIterator = this.slowPorts.iterator();
                    this.portProbeCount.remove(portNumber);

                    // Remove link from topology
                    final PhysicalPort srcPort = this.sw.getPort(portNumber);
                    final PhysicalPort dstPort = PhysicalNetwork.getInstance()
                            .getNeighborPort(srcPort);
                    PhysicalNetwork.getInstance().removeLink(srcPort, dstPort);
                }
            }

            // send a probe for the next slow port
            if (this.slowPorts.size() > 0) {
                if (!this.slowIterator.hasNext()) {
                    this.slowIterator = this.slowPorts.iterator();
                }
                if (this.slowIterator.hasNext()) {
                    final short portNumber = this.slowIterator.next();
                    this.log.debug("sending slow probe to port {}", portNumber);
                    try {
                        OFPacketOut pkt = this.createLLDPPacketOut(this.sw
                                .getPort(portNumber));
                        this.sendMsg(pkt, this);
                        if (useBDDP) {
                            OFPacketOut bpkt = this.createBDDPPacketOut(this.sw
                                    .getPort(portNumber));
                            this.sendMsg(bpkt, this);
                        }
                    } catch (PortMappingException e) {
                        log.warn(e.getMessage());
                    }
                }
            }
        }

        // reschedule timer
        PhysicalNetwork.getTimer().newTimeout(this, this.probeRate,
                TimeUnit.MILLISECONDS);
    }

}
