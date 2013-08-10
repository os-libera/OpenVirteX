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

import java.util.HashMap;
import java.util.HashSet;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.linkdiscovery.LLDPEventHandler;

public abstract class Network<T1, T2, T3> implements LLDPEventHandler,
        OVXSendMsg {

    protected HashSet<T1>              switchSet;
    protected HashSet<T3>              linkSet;
    protected HashMap<Long, T1>        dpidMap;
    protected HashMap<T2, T2>          neighbourPortMap;
    protected HashMap<T1, HashSet<T1>> neighbourMap;

    protected Network() {
	this.switchSet = new HashSet();
	this.linkSet = new HashSet();
	this.dpidMap = new HashMap();
	this.neighbourPortMap = new HashMap();
	this.neighbourMap = new HashMap();
    }

    protected void addLink(final T3 link) {
	System.out.println("K " + this.neighbourMap.keySet());
	System.out.println("V " + this.neighbourMap.values());
	// Actual link creation is in child classes, because creation of generic
	// types sucks
	// Update the linkSet
	this.linkSet.add(link);
	// Update the neighbourMap
	final T1 srcSwitch = (T1) ((Link) link).getSrcSwitch();
	final T1 dstSwitch = (T1) ((Link) link).getDstSwitch();
	final HashSet<T1> neighbours = this.neighbourMap.get(srcSwitch);
	System.out.println("1 " + neighbours);
	neighbours.add(dstSwitch);
	System.out.println("2 " + neighbours);
    }

    protected void addSwitch(final T1 sw) {
	if (this.switchSet.add(sw)) {
	    this.dpidMap.put(((Switch) sw).getSwitchId(), sw);
	    this.neighbourMap.put(sw, new HashSet());
	}
    }

    protected void removeSwitch(final T1 sw) {
	this.dpidMap.remove(((Switch) sw).getSwitchId());
	// TODO: remove ports
	this.switchSet.remove(sw);
    }

    public boolean initialize() {
	return true;
    }

    public HashSet<T1> getNeighbours(final T1 sw) {
	return this.neighbourMap.get(sw);
    }

    public T1 getSwitch(final Long dpid) {
	return this.dpidMap.get(dpid);
    }
}
