/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;


import java.util.Collections;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.Devirtualizable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;


public class OVXSingleSwitch extends OVXSwitch {

    
    private static Logger log = LogManager.getLogger(OVXSingleSwitch.class.getName());
    
    public OVXSingleSwitch(final long switchId, final int tenantId) {
	super(switchId, tenantId);
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
	if (this.isConnected)
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
	// TODO: Release any acquired resources
	channel.disconnect();

    }
    @Override
    public void sendSouth(OFMessage msg) {
	if (physicalSwitchList.size() != 1) {
	    log.error("Virtual single switch {} does not map to a single physical switch", this.getName());
	    return;
	}
	PhysicalSwitch sw = this.physicalSwitchList.get(0);
	sw.sendMsg(msg, sw);
    }
}
