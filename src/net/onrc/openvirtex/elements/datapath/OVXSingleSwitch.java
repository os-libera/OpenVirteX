/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;


import java.util.Collections;
import java.util.List;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IllegalVirtualSwitchConfiguration;
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
     * @return the port instance
     */
    @Override
    protected OVXPort getPort(Short portNumber) {
	return this.portMap.get(portNumber);
    }

    @Override
    public boolean registerPort(Short ovxPortNumber, Long physicalSwitchId,
            Short physicalPortNumber) throws IllegalVirtualSwitchConfiguration {
	
	OVXPort ovxPort = getPort(ovxPortNumber);
	List<PhysicalSwitch> switchList =  OVXMap.getInstance().getPhysicalSwitches(this);
	if (switchList.size() > 1)
	    throw new IllegalVirtualSwitchConfiguration("Switch " + this.switchId + 
		    " is a single switch made up of multiple physical switches");
	PhysicalSwitch physicalSwitch = switchList.get(0);
	
	
	assert(physicalSwitchId == this.switchId);
	
	PhysicalPort physicalPort = physicalSwitch.getPort(physicalPortNumber);

	// Map the two ports
	ovxPort.setPhysicalPort(physicalPort);
	physicalPort.setOVXPort(this.tenantId, ovxPort);
	// If the ovxPort is an edgePort, set also the physicalPort as an edge
	
	physicalPort.setIsEdge(ovxPort.getIsEdge());
	
	return true;
    };

}
