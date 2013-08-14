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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.linkdiscovery.SwitchDiscoveryManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;

/**
 * 
 * Singleton class for physical network. Maintains SwitchDiscoveryManager for
 * each switch in the physical network. Listens for LLDP packets and passes them
 * on to the appropriate SwitchDiscoveryManager. Creates and maintains links
 * after discovery, and switch ports are made discoverable here.
 * TODO: should probably subscribe to PORT UP/DOWN events here
 * 
 */
public class PhysicalNetwork extends
        Network<PhysicalSwitch, PhysicalPort, PhysicalLink> implements
        TimerTask {

    private static PhysicalNetwork                      instance;
    private ArrayList<Uplink>                           uplinkList;
//    Map<LinkAdvertisement, Long>			latestProbes;
    private final HashMap<Long, SwitchDiscoveryManager> discoveryManager;
    private static HashedWheelTimer                     timer;
    private final long 					updatePeriod = 5000; 	// milliseconds
    private final long					timeoutPeriod = 10000;	// milliseconds
    Logger                                              log = LogManager
	                                                            .getLogger(PhysicalNetwork.class
	                                                                    .getName());

    private PhysicalNetwork() {
	this.log.info("Starting network discovery...");
	PhysicalNetwork.timer = new HashedWheelTimer();
	this.discoveryManager = new HashMap<Long, SwitchDiscoveryManager>();
//	this.latestProbes = new HashMap<LinkAdvertisement, Long>();
	
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
	final PhysicalPort neighbourPort = this.neighbourPortMap.get(srcPort);
	if (neighbourPort == null || !neighbourPort.equals(dstPort)) {
	    final PhysicalLink link = new PhysicalLink(srcPort, dstPort);
	    super.addLink(link);
	}
    }

    /**
     * Handle LLDP packets by passing them on to the appropriate
     * SwitchDisoveryManager (which sent the original LLDP packet).
     */
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	// Pass on msg to SwitchDiscoveryManager for that switch
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
    public void run(Timeout t) throws Exception {
//	log.debug("processing updates");
//	for (Iterator<LinkAdvertisement> it = this.latestProbes.keySet()
//			.iterator(); it.hasNext();) {
//		LinkAdvertisement linkAdvertisement = it.next();
//		long now = System.currentTimeMillis();
//		long thisProbe = this.latestProbes.get(linkAdvertisement).longValue();
//		if ((thisProbe + this.timeoutPeriod) < now) {
//			log.debug("timeout: removing timed-out link " + linkAdvertisement);
//			it.remove();
//		}
//	}
//	// Schedule next event
//	timer.newTimeout(this, updatePeriod, TimeUnit.MILLISECONDS);
//	
    }

    @Override
    public boolean boot() {
	timer.newTimeout(this, updatePeriod, TimeUnit.MILLISECONDS);
	return true;
    }

}
