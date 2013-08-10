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

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.linkdiscovery.SwitchDiscoveryManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

public class PhysicalNetwork extends
        Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

    private static PhysicalNetwork                      instance;
    private ArrayList<Uplink>                           uplinkList;
    private final HashMap<Long, SwitchDiscoveryManager> discoveryManager;
    Logger                                              log = LogManager
	                                                            .getLogger(PhysicalNetwork.class
	                                                                    .getName());

    private PhysicalNetwork() {
	log.info("Starting physical network discovery");
	this.discoveryManager = new HashMap<Long, SwitchDiscoveryManager>();
    }

    public static PhysicalNetwork getInstance() {
	if (PhysicalNetwork.instance == null) {
	    PhysicalNetwork.instance = new PhysicalNetwork();
	}
	return PhysicalNetwork.instance;
    }

    public ArrayList<Uplink> getUplinkList() {
	return this.uplinkList;
    }

    public void setUplinkList(final ArrayList<Uplink> uplinkList) {
	this.uplinkList = uplinkList;
    }

    public void addSwitch(final PhysicalSwitch sw) {
	super.addSwitch(sw);
	this.discoveryManager.put(sw.getSwitchId(), new SwitchDiscoveryManager(
	        sw));
    }
    
    public void addPort(final PhysicalPort port) {
	this.discoveryManager.get(port.getParentSwitch().getSwitchId()).addPort(port);
    }

    synchronized public void addLink(final PhysicalPort srcPort, final PhysicalPort dstPort) {
	System.out.println("adding link " + srcPort.toString() + "-" + dstPort.toString());
	if (this.neighbourPortMap.get(srcPort) != dstPort) {
	    PhysicalLink link = new PhysicalLink(srcPort, dstPort);
	    this.neighbourPortMap.put(srcPort, dstPort);
	    super.addLink(link);
	}
    }
    
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
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

}
