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

package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import java.util.HashMap;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * The Class PhysicalLink.
 * 
 */

public class PhysicalLink extends Link<PhysicalPort,PhysicalSwitch> {

    /**
     * Instantiates a new physical link.
     * 
     * @param srcPort
     *            the source port
     * @param dstPort
     *            the destination port
     */
    public PhysicalLink(final PhysicalPort srcPort, final PhysicalPort dstPort) {
	super(srcPort, dstPort);
    }
    
    public HashMap<String,Object> toJson() {
	HashMap<String,Object> output = new HashMap<String,Object>();
	
	HashMap<String,Object> srcMap = new HashMap<String,Object>();
	HashMap<String,Object> dstMap = new HashMap<String,Object>();
	srcMap.put(SWID,String.valueOf(srcPort.getParentSwitch().getSwitchId()));
	srcMap.put(PORTNUM,String.valueOf(srcPort.getPortNumber()));
	dstMap.put(SWID,String.valueOf(dstPort.getParentSwitch().getSwitchId()));
	dstMap.put(PORTNUM,String.valueOf(dstPort.getPortNumber()));
	
	//list.add(ovxMap);
	output.put(SRC, srcMap);
	output.put(DST, dstMap);
	return output; 
    }

}
