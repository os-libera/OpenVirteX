/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IllegalVirtualSwitchConfiguration;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.routing.RoutingAlgorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

/**
 * The Class OVXBigSwitch.
 * 
 */

public class OVXBigSwitch extends OVXSwitch {

    private static Logger log = LogManager.getLogger(OVXBigSwitch.class.getName());
    
    /** The alg. */
    private RoutingAlgorithms alg;

    /** The path map. */
    private final HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>> pathMap;
    

    public OVXBigSwitch(final long switchId, final int tenantId, final List<PhysicalSwitch> physicalSwitches) {
	super(switchId, tenantId);
	this.alg = RoutingAlgorithms.NONE;
	this.pathMap = new HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>>();
	this.physicalSwitchList.addAll(physicalSwitches);
    }
    
    /**
     * Gets the alg.
     * 
     * @return the alg
     */
    public RoutingAlgorithms getAlg() {
	return this.alg;
    }

    /**
     * Sets the alg.
     * 
     * @param alg
     *            the new alg
     */
    public void setAlg(final RoutingAlgorithms alg) {
	this.alg = alg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#updatePort(java.lang.Object)
     */
    @Override
    public boolean updatePort(final OVXPort port) {
	if (!this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#removePort(short)
     */
    @Override
    public boolean removePort(final Short portNumber) {
	if (!this.portMap.containsKey(portNumber)) {
	    return false;
	} else {
	    this.portMap.remove(portNumber);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.
     * OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
     */
    @Override
    public void sendMsg(OFMessage msg, OVXSendMsg from) {
	// TODO Truncate the message for the ctrl to the missSetLenght value
	channel.write(Collections.singletonList(msg));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(OFMessage msg) {
	try {
	    ((Devirtualizable) msg).devirtualize(this);
	} catch (ClassCastException e) {
	    log.error("Received illegal message : " + msg);
	}

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
     */
    @Override
    public void tearDown() {
	//TODO: Release any acquired resources.
	channel.disconnect();

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#init()
     */
    @Override
    public void init() {
	generateFeaturesReply();
//	OVXNetwork net = OVXMap.getInstance().getVirtualNetwork(this.tenantId);
//	OpenVirteXController.getInstance().registerOVXSwitch(this, 
//		net.getControllerHost(), net.getControllerPort());
	// TODO: Start the internal routing protocol
    }

    /**
     * Gets the port.
     * 
     * @param portNumber
     *            the port number
     * @return the port instance
     */
    @Override
    public OVXPort getPort(Short portNumber) {
	return this.portMap.get(portNumber);
    };

    @Override
    public String toString() {
	return "SWITCH:\n- switchId: " + this.switchId + "\n- switchName: "
		+ this.switchName + "\n- isConnected: " + this.isConnected
		+ "\n- tenantId: " + this.tenantId + "\n- missSendLenght: "
		+ this.missSendLen + "\n- isActive: " + this.isActive
		+ "\n- capabilities: "
		+ this.capabilities.getOVXSwitchCapabilities()
		+ "\n- algorithm: " + this.alg.getValue();
    }
}
