/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.HashMap;
import java.util.LinkedList;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.Devirtualizable;
import net.onrc.openvirtex.routing.RoutingAlgorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

/**
 * The Class OVXBigSwitch.
 * 
 * @author gerola
 */

public class OVXBigSwitch extends OVXSwitch {

    private static Logger log = LogManager.getLogger(OVXBigSwitch.class.getName());
    
    /** The alg. */
    private RoutingAlgorithms alg;

    /** The path map. */
    private final HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>> pathMap;

    /**
     * Instantiates a new oVX big switch.
     */
    public OVXBigSwitch() {
	super();
	this.alg = RoutingAlgorithms.NONE;
	this.pathMap = new HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>>();
    }

    /**
     * Instantiates a new oVX big switch.
     * 
     * @param switchName
     *            the switch name
     * @param switchId
     *            the switch id
     * @param tenantId
     *            the tenant id
     * @param pktLenght
     *            the pkt lenght
     * @param alg
     *            the alg
     */
    public OVXBigSwitch(final long switchId, final int tenantId,
	    final RoutingAlgorithms alg) {
	super(switchId, tenantId);
	this.alg = alg;
	this.pathMap = new HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>>();
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
     * net.onrc.openvirtex.elements.datapath.Switch#addPort(java.lang.Object)
     */
    @Override
    public boolean addPort(final OVXPort port) {
	if (this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
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
	// TODO Auto-generated method stub
	// Truncate the message for the ctrl to the missSetLenght value

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(OFMessage msgs) {
	try {
	    ((Devirtualizable) msgs).devirtualize(this);
	} catch (ClassCastException e) {
	    log.error("Received illegal message : " + msgs);
	}

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
     */
    @Override
    public void tearDown() {
	// TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#init()
     */
    @Override
    public void init() {
	generateFeaturesReply();
	// TODO: Register to the upper loop
	// TODO: Start the internal routing protocol

    }

    @Override
    public boolean setSwitchId(Long switchId) {
	this.switchId = switchId;
	return true;
    }

    /**
     * Gets the port.
     * 
     * @param portNumber
     *            the port number
     * @return a COPY of the port instance
     */
    @Override
    public OVXPort getPort(Short portNumber) {
	return this.portMap.get(portNumber).clone();
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
