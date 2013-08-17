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
import net.onrc.openvirtex.messages.OVXMessageFactory;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;

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
 * Run discovery process from a physical switch.
 * Based on FlowVisor topology discovery implementation.
 * 
 * TODO: add 'fast discovery' mode: drop LLDPs in destination switch but
 * listen for flow_removed messages
 */
public class SwitchDiscoveryManager implements LLDPEventHandler, OVXSendMsg,
        TimerTask {

    private final PhysicalSwitch            sw;
    // this is the safety rate: this many
    private final long                      probeRate;
    private final Set<Short>                slowPorts;
    private final Set<Short>                fastPorts;
    private final Object                    portLock;
    private final Map<Short, AtomicInteger> portProbeCount;
    private final static short              MAX_PROBE_COUNT   = 3;                        // number
	                                                                                   // of
	                                                                                   // probes
	                                                                                   // to
	                                                                                   // send
	                                                                                   // before
	                                                                                   // link
	                                                                                   // is
	                                                                                   // removed
    private Iterator<Short>                 slowIterator;
    private final OVXMessageFactory         ovxMessageFactory = OVXMessageFactory
	                                                              .getInstance();
    Logger                                  log               = LogManager
	                                                              .getLogger(SwitchDiscoveryManager.class
	                                                                      .getName());

    public SwitchDiscoveryManager(final PhysicalSwitch sw) {
	this.sw = sw;
	this.probeRate = 1000;		// send 1 probe every probeRate milliseconds
	this.slowPorts = Collections.synchronizedSet(new HashSet<Short>());
	this.fastPorts = Collections.synchronizedSet(new HashSet<Short>());
	this.portLock = new Object();
	this.portProbeCount = new HashMap<Short, AtomicInteger>();
	PhysicalNetwork.getTimer().newTimeout(this, this.probeRate,
	        TimeUnit.MILLISECONDS);
	this.log.debug("Started discovery manager for switch {}",
	        sw.getSwitchId());
    }

    /**
     * Add port to discovery process. Send out initial LLDP and label it as slow
     * port.
     * 
     * @param port
     */
    public void addPort(final PhysicalPort port) {
	// Ignore ports that are not on this switch
	if (port.getParentSwitch().equals(this.sw)) {
	    synchronized (this) {
		this.log.debug("sending init probe to port {}",
		        port.getPortNumber());
		final OFPacketOut pkt = this.createLLDPPacketOut(port);
		this.sendMsg(pkt, this);
		this.slowPorts.add(port.getPortNumber());
		this.slowIterator = this.slowPorts.iterator();
	    }
	}
    }

    /**
     * Remove port from discovery process
     * 
     * @param port
     */
    public void removePort(final PhysicalPort port) {
	// Ignore ports that are not on this switch
	if (port.getParentSwitch().equals(this.sw)) {
	    synchronized (this) {
		if (this.slowPorts.contains(port)) {
		    this.slowPorts.remove(port);
		    this.slowIterator = this.slowPorts.iterator();

		} else
		    if (this.fastPorts.contains(port)) {
			this.fastPorts.remove(port);
			this.portProbeCount.remove(port.getPortNumber());
			// no iterator to update
		    } else {
			this.log.warn(
			        "tried to dynamically remove non-existing port {}",
			        port.getPortNumber());
		    }
	    }
	}
    }

    private void signalSlowPort(final PhysicalPort port) {
	    final short portNumber = port.getPortNumber();

	    if (this.fastPorts.contains(portNumber)) {
		this.log.debug("Setting fast port to slow: {}:{}", port
		        .getParentSwitch().getSwitchId(), portNumber);
		this.fastPorts.remove(portNumber);
		this.slowPorts.add(portNumber);
		// surprised that this could be solution?!
		this.slowIterator = this.slowPorts.iterator();
		this.portProbeCount.remove(portNumber);
	    } else {
		if (!this.slowPorts.contains(portNumber)) {
		    this.log.debug(
			    "Got signalSlowPort for non-existing port: {}",
			    portNumber);
		}
	    

	}
    }

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
     * @param port
     * @return
     *         Packet_out message with LLDP data
     */
    private OFPacketOut createLLDPPacketOut(final PhysicalPort port) {
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
	final byte[] lldp = LLDPUtil.makeLLDP(port);
	packetOut.setActionsLength(alen);
	packetOut.setPacketData(lldp);
	packetOut
	        .setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + lldp.length));
	return packetOut;
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	this.sw.sendMsg(msg, this);
    }

    /**
     * Count the number of actions in an actionsList
     * TODO: why is this needed? just use actionsList.size()?
     * 
     * @param actionsList
     * @return
     *         The number of actions
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

    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final OVXPacketIn pi = (OVXPacketIn) msg;
	final byte[] pkt = pi.getPacketData();
	if (LLDPUtil.checkLLDP(pkt)) {
	    // TODO: check if dpid present
	    final PhysicalPort dstPort = (PhysicalPort) sw.getPort(pi
		    .getInPort());
	    final DPIDandPort dp = LLDPUtil.parseLLDP(pkt);
	    final PhysicalSwitch srcSwitch = PhysicalNetwork.getInstance()
		    .getSwitch(dp.getDpid());
	    final PhysicalPort srcPort = srcSwitch.getPort(dp.getPort());

	    PhysicalNetwork.getInstance().createLink(srcPort, dstPort);
	    PhysicalNetwork.getInstance().ackProbe(srcPort);
	} else {
	    this.log.debug("Invalid LLDP");
	}
    }

    @Override
    public void run(final Timeout t) throws Exception {
	this.log.debug("sending probes");

	synchronized (this) {

	    // send a probe per fast port
	    Iterator<Short> fastIterator = this.fastPorts.iterator();
	    while (fastIterator.hasNext()) {
		Short portNumber = fastIterator.next();
		final int probeCount = this.portProbeCount.get(portNumber)
		        .getAndIncrement();
		if (probeCount < SwitchDiscoveryManager.MAX_PROBE_COUNT) {
		    this.log.debug("sending fast probe to port");
		    final OFPacketOut pkt = this.createLLDPPacketOut(this.sw
			    .getPort(portNumber));
		    this.sendMsg(pkt, this);
		} else {
		    // Update fast and slow ports
		    fastIterator.remove();
		    this.slowPorts.add(portNumber);
		    this.slowIterator = this.slowPorts.iterator();
		    this.portProbeCount.remove(portNumber);
		    
		    //this.signalSlowPort(srcPort);
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
		    final OFPacketOut pkt = this.createLLDPPacketOut(this.sw
			    .getPort(portNumber));
		    this.sendMsg(pkt, this);
		}
	    }

	}

	// reschedule timer
	PhysicalNetwork.getTimer().newTimeout(this, this.probeRate,
	        TimeUnit.MILLISECONDS);

    }

}
