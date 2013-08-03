/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.Devirtualizable;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

/**
 * @author gerola
 * 
 */
public class OVXSingleSwitch extends OVXSwitch {

    
    private static Logger log = LogManager.getLogger(OVXSingleSwitch.class.getName());
    /**
     * 
     */
    public OVXSingleSwitch() {
	super();
    }

    /**
     * @param switchName
     * @param switchId
     * @param map
     * @param tenantId
     * @param pktLenght
     */
    public OVXSingleSwitch(final long switchId, final int tenantId) {
	super(switchId, tenantId);
    }

    @Override
    public boolean addPort(final OVXPort port) {
	if (this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    @Override
    public boolean updatePort(final OVXPort port) {
	if (!this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

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

}
