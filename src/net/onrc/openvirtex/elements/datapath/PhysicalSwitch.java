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

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import java.util.Collections;

import net.onrc.openvirtex.core.io.OVXSendMsg;

import net.onrc.openvirtex.messages.Virtualizable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

  
public class PhysicalSwitch extends Switch<PhysicalPort> {
    
    Logger log =  LogManager.getLogger(PhysicalSwitch.class.getName());
    
    /**
     * 
     */
    public PhysicalSwitch() {
	super();
    }

    /**
     * @param switchName
     * @param switchId
     * @param map
     */
    public PhysicalSwitch(final String switchName, final long switchId,
	    final OVXMap map) {
	super(switchName, switchId, map);
    }

    @Override
    public boolean initialize() {
	//TODO: Take featuresReply and add ports to the maps
	return false;
    }

    @Override
    public synchronized void handleIO(OFMessage msgs) {
	try {
	    ((Virtualizable) msgs).virtualize(this);
	} catch (ClassCastException e) {
	    log.error("Received illegal message : " + msgs);
	}
	
    }

    @Override
    public void tearDown() {
	log.info("Switch disconnected {} ", this.featuresReply.getDatapathId());
		
    }

    @Override
    public void init() {
	log.info("Switch connected {} : {}", this.featuresReply.getDatapathId(), this.desc.getHardwareDescription());
	
    }

    
    /*
     * Temporary implementation(non-Javadoc)
     * @see net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
     */
    @Override
    public void sendMsg(OFMessage msg, OVXSendMsg from) {
	channel.write(Collections.singletonList(msg));
    }
    
    @Override
    public String toString() {
	return "DPID : " + this.featuresReply.getDatapathId() + 
		", remoteAddr : " + this.channel.getRemoteAddress().toString();
    }

    @Override
    public boolean setSwitchId(long switchId) {
	return false;
    }

}
