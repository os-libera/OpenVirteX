/**
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package net.onrc.openvirtex.elements.network;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {

    // public VLinkManager vLinkMgmt;

    private final Integer                  tenantId;
    private final String                   protocol;
    private final String                   controllerHost;
    private final Integer                  controllerPort;
    private final IPAddress                      network;
    private final short                          mask;
    private HashMap<IPAddress, MACAddress> gwsMap;
    private boolean                        bootState;
    private AtomicInteger		linkCounter;

    Logger                                 log = LogManager
	                                               .getLogger(OVXNetwork.class
	                                                       .getName());

    public OVXNetwork(final Integer tenantId, final String protocol,
	    final String controllerHost, final Integer controllerPort,
	    final IPAddress network, final short mask) {
	super();
	this.tenantId = tenantId;
	this.protocol = protocol;
	this.controllerHost = controllerHost;
	this.controllerPort = controllerPort;
	this.network = network;
	this.mask = mask;
	this.bootState = false;
	// TODO: decide which value to start linkId's
	this.linkCounter = new AtomicInteger(2);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getControllerHost() {
	return this.controllerHost;
    }

    public Integer getControllerPort() {
	return this.controllerPort;
    }

    public Integer getTenantId() {
	return this.tenantId;
    }

    public IPAddress getNetwork() {
	return this.network;
    }

    public short getMask() {
	return this.mask;
    }

    public void register() {
	OVXMap.getInstance().addNetwork(this);
    }

    public OVXSwitch createSwitch() {
	OVXBigSwitch sw = new OVXBigSwitch();
	super.addSwitch(sw);
	// start discovery
	return sw;
    }

    public Integer createLink(OVXPort srcPort, OVXPort dstPort) {
	System.out.println("adding link " + srcPort.toString() + "-" + dstPort.toString());
	int linkId = -1;
	if (!this.neighbourPortMap.containsKey(srcPort)) {
	    linkId = this.linkCounter.getAndIncrement(); 
	    OVXLink link = new OVXLink(srcPort, dstPort, this.tenantId, linkId);	    
	    this.neighbourPortMap.put(srcPort, dstPort);
	    super.addLink(link);
	}
	return linkId;
    }

    public OVXPort createHost(final PhysicalPort port) {
	return new OVXPort();
    }

    public void createGateway(final IPAddress ip) {

    }

    public boolean boot() {
	this.bootState = true;
	return this.bootState;
    }

    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final OVXPacketOut po = (OVXPacketOut) msg;
	final byte[] pkt = po.getPacketData();
	if (LLDPUtil.checkLLDP(pkt)) {
	    // Get input port from pkt_out
	    for (final OFAction action : po.getActions()) {
		try {
		    final short portNumber = ((OFActionOutput) action).getPort();
		    final OVXPort srcPort = (OVXPort) sw.getPort(portNumber);
		    final OVXPort dstPort = this.neighbourPortMap.get(srcPort);
		    final OVXPacketIn pi = new OVXPacketIn();
		    pi.setInPort(dstPort.getPortNumber());
		    pi.setPacketData(pkt);
		    dstPort.getParentSwitch().sendMsg(pi, this);
		} catch (ClassCastException c) {
		    // ignore non-ActionOutput pkt_out's 
		}
	    }
	} else {
	    this.log.debug("Invalid LLDP");
	}
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	// Do nothing
    }

    @Override
    public String getName() {
	return "Virtual network:" + this.tenantId.toString();
    }
}
