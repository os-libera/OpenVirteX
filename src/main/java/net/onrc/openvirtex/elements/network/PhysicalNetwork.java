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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.linkdiscovery.SwitchDiscoveryManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.openflow.protocol.OFMessage;

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
public class PhysicalNetwork extends
        Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

    private static PhysicalNetwork                  instance;
    private ArrayList<Uplink>                       uplinkList;
    private final Map<Long, SwitchDiscoveryManager> discoveryManager;
    private static HashedWheelTimer                 timer;
    Logger                                          log           = LogManager
	                                                                  .getLogger(PhysicalNetwork.class
	                                                                          .getName());

    private PhysicalNetwork() {
	this.log.info("Starting network discovery...");
	PhysicalNetwork.timer = new HashedWheelTimer();
	this.discoveryManager = new HashMap<Long, SwitchDiscoveryManager>();
    }

    public static PhysicalNetwork getInstance() {
	if (PhysicalNetwork.instance == null) {
	    PhysicalNetwork.instance = new PhysicalNetwork();
	}
	return PhysicalNetwork.instance;
    }

    public static HashedWheelTimer getTimer() {
	return PhysicalNetwork.timer;
    }

    public ArrayList<Uplink> getUplinkList() {
	return this.uplinkList;
    }

    public void setUplinkList(final ArrayList<Uplink> uplinkList) {
	this.uplinkList = uplinkList;
    }

    /**
     * Add switch to topology and make discoverable
     */
    @Override
    public synchronized void addSwitch(final PhysicalSwitch sw) {
	super.addSwitch(sw);
	this.discoveryManager.put(sw.getSwitchId(), new SwitchDiscoveryManager(
	        sw));
    }

    /**
     * Add port for discovery
     * 
     * @param port
     */
    public synchronized void addPort(final PhysicalPort port) {
	this.discoveryManager.get(port.getParentSwitch().getSwitchId())
	        .addPort(port);
    }

    /**
     * Create link and add it to the topology.
     * 
     * @param srcPort
     * @param dstPort
     */
    public synchronized void createLink(final PhysicalPort srcPort,
	    final PhysicalPort dstPort) {
	final PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
	if (neighbourPort == null || !neighbourPort.equals(dstPort)) {
	    final PhysicalLink link = new PhysicalLink(srcPort, dstPort);
	    super.addLink(link);
	} else {
	    this.log.debug("Tried to create invalid link");
	}
    }

    /**
     * Create link and add it to the topology.
     * 
     * @param srcPort
     * @param dstPort
     */
    public synchronized void removeLink(final PhysicalPort srcPort,
	    final PhysicalPort dstPort) {
	final PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
	if (neighbourPort.equals(dstPort)) {
	    final PhysicalLink link = super.getLink(srcPort, dstPort);
	    super.removeLink(link);
	} else {
	    this.log.debug("Tried to remove invalid link");
	}
    }

    /**
     * Acknowledge reception of discovery probe to sender port
     * 
     * @param port
     */
    public void ackProbe(final PhysicalPort port) {
	final SwitchDiscoveryManager sdm = this.discoveryManager.get(port
	        .getParentSwitch().getSwitchId());
	if (sdm != null) {
	    sdm.ackProbe(port);
	}
    }

    /**
     * Handle LLDP packets by passing them on to the appropriate
     * SwitchDisoveryManager (which sent the original LLDP packet).
     */
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

}
