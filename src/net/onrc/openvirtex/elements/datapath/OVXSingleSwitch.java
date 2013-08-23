/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.messages.Devirtualizable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

public class OVXSingleSwitch extends OVXSwitch {

    private static Logger log = LogManager.getLogger(OVXSingleSwitch.class
	                              .getName());

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
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	if (this.isConnected) {
	    this.channel.write(Collections.singletonList(msg));
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(final OFMessage msg) {
	try {
	    ((Devirtualizable) msg).devirtualize(this);
	} catch (final ClassCastException e) {
	    OVXSingleSwitch.log.error("Received illegal message : " + msg);
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
	this.channel.disconnect();

    }

    @Override
    // TODO: this is probably not optimal
    public void sendSouth(final OFMessage msg) {
	final PhysicalSwitch sw = OVXMap.getInstance()
	        .getPhysicalSwitches(this).get(0);
	sw.sendMsg(msg, this);
    }
}
