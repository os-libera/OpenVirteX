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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.linkdiscovery.LLDPEventHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

/**
 * 
 * Abstract parent class for networks, maintains data structures for the
 * topology graph.
 * 
 * @param <T1>
 *            Generic Switch type
 * @param <T2>
 *            Generic Port type
 * @param <T3>
 *            Generic Link type
 */
public abstract class Network<T1, T2, T3> implements LLDPEventHandler,
        OVXSendMsg {

    protected HashSet<T1>              switchSet;
    protected HashSet<T3>              linkSet;
    protected HashMap<Long, T1>        dpidMap;
    protected HashMap<T2, T2>          neighbourPortMap;
    protected HashMap<T1, HashSet<T1>> neighbourMap;

    Logger                             log = LogManager.getLogger(Network.class
	                                           .getName());

    protected Network() {
	this.switchSet = new HashSet<T1>();
	this.linkSet = new HashSet<T3>();
	this.dpidMap = new HashMap<Long, T1>();
	this.neighbourPortMap = new HashMap<T2, T2>();
	this.neighbourMap = new HashMap<T1, HashSet<T1>>();
    }

    // Protected methods to update topology (only allowed from subclasses)

    /**
     * Add link to topology
     * 
     * @param link
     */
    protected void addLink(final T3 link) {
	// Actual link creation is in child classes, because creation of generic
	// types sucks
	this.linkSet.add(link);
	final T1 srcSwitch = (T1) ((Link) link).getSrcSwitch();
	final T1 dstSwitch = (T1) ((Link) link).getDstSwitch();
	final HashSet<T1> neighbours = this.neighbourMap.get(srcSwitch);
	neighbours.add(dstSwitch);
	this.neighbourPortMap.put((T2) ((Link) link).getSrcPort(),
	        (T2) ((Link) link).getDstPort());
	this.log.info("Adding link " + link.toString());
    }

    /**
     * Add switch to topology
     * 
     * @param sw
     */
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

    // Public methods to query topology information


    /**
     * Return neighbours of switch.
     * 
     * @param sw
     * @return
     *         Unmodifiable set of switch instances
     */
    public Set<T1> getNeighbours(final T1 sw) {
	return Collections.unmodifiableSet(this.neighbourMap.get(sw));
    }


    /**
     * Return switch instance based on its dpid
     * 
     * @param dpid
     * @return
     */
    public T1 getSwitch(final Long dpid) {
	return this.dpidMap.get(dpid);
    }
    
    public Set<T1> getSwitches() {
	return Collections.unmodifiableSet(this.switchSet);
    }
    
    // TODO: optimize this because we iterate over all links
    public T3 getLink(T2 srcPort, T2 dstPort) {
	for (T3 link : this.linkSet) {
	    if (((Link) link).getSrcPort().equals(srcPort) && ((Link) link).getDstPort().equals(dstPort)) {
		return link;
	    }
	}
	return null;
    }
}
