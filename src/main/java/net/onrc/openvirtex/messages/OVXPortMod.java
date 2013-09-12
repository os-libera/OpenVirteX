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

package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFPortMod;

public class OVXPortMod extends OFPortMod implements Devirtualizable {

	@Override
	public void devirtualize(OVXSwitch sw) {
	    // TODO Auto-generated method stub
	    //assume port numbers are virtual
	    OVXPort p = sw.getPort(this.getPortNumber());
	    if (p == null) {
		sw.sendMsg(OVXMessageUtil.makeErrorMsg(
			OFBadRequestCode.OFPBRC_EPERM, this), sw);
		return;
	    }
	    //set physical port number - anything else to do?
	    PhysicalPort phyPort = p.getPhysicalPort();
	    this.setPortNumber(phyPort.getPortNumber());
	    
	    OVXMessageUtil.translateXid(this, p);
	}

}
