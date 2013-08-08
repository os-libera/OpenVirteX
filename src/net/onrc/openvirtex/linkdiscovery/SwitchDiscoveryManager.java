package net.onrc.openvirtex.linkdiscovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXMessageFactory;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

public class SwitchDiscoveryManager implements LLDPEventHandler, OVXSendMsg {

    private final TopologyDiscoveryManager topologyDiscoveryManager;
    private final PhysicalSwitch           sw;
    private final long                     probesPerPeriod;                              // this
	                                                                                  // is
	                                                                                  // the
	                                                                                  // safety
	                                                                                  // rate:
	                                                                                  // this
	                                                                                  // many
    private final long                     fastProbeRate;
    private final Set<Short>               slowPorts;
    private final Set<Short>               fastPorts;
    private Iterator<Short>                slowIterator;
    Logger                                 log               = LogManager
	                                                             .getLogger(SwitchDiscoveryManager.class
	                                                                     .getName());
    private final OVXMessageFactory        ovxMessageFactory = OVXMessageFactory
	                                                             .getInstance();

    public SwitchDiscoveryManager(
	    final TopologyDiscoveryManager topologyDiscoveryManager,
	    final PhysicalSwitch sw) {
	this.topologyDiscoveryManager = topologyDiscoveryManager;
	this.sw = sw;
	this.probesPerPeriod = 3;
	this.fastProbeRate = this.topologyDiscoveryManager.getUpdatePeriod()
	        / this.probesPerPeriod;
	this.slowPorts = new HashSet<Short>();
	this.fastPorts = new HashSet<Short>();
    }

    // assume this is entry point after waking up
    public void run() {
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
	// this.pollLoop.addTimer(new FVTimerEvent(System.currentTimeMillis()
	// + this.fastProbeRate, this, this, null));
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
	// register link in topology
	// reset timer
    }

}
