package net.onrc.openvirtex.linkdiscovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXMessageFactory;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

public class SwitchDiscoveryManager implements LLDPEventHandler, OVXSendMsg,
        TimerTask {

    private final PhysicalSwitch    sw;
    // this is the safety rate: this many
    private final long              probesPerPeriod;
    private final long              fastProbeRate;
    private final Set<Short>        slowPorts;
    private final Set<Short>        fastPorts;
    private Iterator<Short>         slowIterator;
    Logger                          log               = LogManager
	                                                      .getLogger(SwitchDiscoveryManager.class
	                                                              .getName());
    private final OVXMessageFactory ovxMessageFactory = OVXMessageFactory.getInstance();
    
    private final HashedWheelTimer  timer;

    public SwitchDiscoveryManager(final PhysicalSwitch sw) {
	this.sw = sw;
	this.probesPerPeriod = 3;
	this.fastProbeRate = 5000 / this.probesPerPeriod;
	this.slowPorts = new HashSet<Short>();
	this.fastPorts = new HashSet<Short>();
	this.timer = new HashedWheelTimer();
	this.timer.newTimeout(this, this.fastProbeRate, TimeUnit.MILLISECONDS);
	log.debug("Started discovery manager for switch {}", sw.getSwitchId().toString());
    }

    synchronized public void addPort(final PhysicalPort port) {
	// this function is synchronized so it shouldn't get hosed
	this.log.debug("sending init probe to port {}", port.getPortNumber());
	final OFPacketOut pkt = this.createLLDPPacketOut(port);
	this.sendMsg(pkt, this);
	this.slowPorts.add(port.getPortNumber());
	this.slowIterator = this.slowPorts.iterator();
    }

    synchronized public void removePort(final PhysicalPort port) {
	// this function is synchronized so it shouldn't get hosed
	if (this.slowPorts.contains(port)) {
	    this.slowPorts.remove(port);
	    this.slowIterator = this.slowPorts.iterator();
	} else
	    if (this.fastPorts.contains(port)) {
		this.fastPorts.remove(port);
		// no iterator to update
	    } else {
		this.log.warn(
		        "tried to dynamically remove non-existant port {}",
		        port.getPortNumber());
	    }
    }

    private OFPacketOut createLLDPPacketOut(final PhysicalPort port) {
	final OFPacketOut packetOut = (OFPacketOut) this.ovxMessageFactory
	        .getMessage(OFType.PACKET_OUT);
	packetOut.setBufferId(-1);
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
	    final PhysicalPort dstPort =  (PhysicalPort) sw.getPort(pi.getInPort());
	    final DPIDandPort dp = LLDPUtil.parseLLDP(pkt);
	    final PhysicalSwitch srcSwitch = PhysicalNetwork.getInstance().getSwitch(dp.getDpid());
	    final PhysicalPort srcPort = srcSwitch.getPort(dp.getPort());
	    PhysicalNetwork.getInstance().addLink(srcPort, dstPort);
	} else {
	    this.log.debug("Invalid LLDP");
	}	
	// register link in topology
	// reset timer
    }

    @Override
    public void run(final Timeout t) throws Exception {
	this.log.debug("sending probes");
	// send a probe per fast port
	for (final Short portNumber : this.fastPorts) {
	    this.log.debug("sending fast probe to port");
	    final OFPacketOut pkt = this.createLLDPPacketOut(this.sw
		    .getPort(portNumber));
	    this.sendMsg(pkt, this);
	}

	// send a probe for the next slow port
	if (this.slowPorts.size() > 0) {
	    if (!this.slowIterator.hasNext()) {
		this.slowIterator = this.slowPorts.iterator();
	    }
	    if (this.slowIterator.hasNext()) {
		final short portNumber = this.slowIterator.next();
		this.log.debug("sending slow probe to port");
		final OFPacketOut pkt = this.createLLDPPacketOut(this.sw
		        .getPort(portNumber));
		this.sendMsg(pkt, this);
	    }
	}
	// reschedule timer
	this.timer.newTimeout(this, this.fastProbeRate, TimeUnit.MILLISECONDS);
    }

}
