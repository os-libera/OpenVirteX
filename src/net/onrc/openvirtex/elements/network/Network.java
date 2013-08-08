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

import java.util.ArrayList;
import java.util.HashMap;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.linkdiscovery.LLDPEventHandler;

public abstract class Network<T1, T2, T3> implements LLDPEventHandler,
        OVXSendMsg {

    protected ArrayList<T1>              switchList;
    protected HashMap<Long, T1>          dpidMap;
    protected ArrayList<T3>              linkList;
    protected HashMap<T2, T2>            neighbourPortMap;
    protected HashMap<T1, ArrayList<T1>> neighbourMap;

    // public OFControllerChannel channel;

    protected Network() {
	final ArrayList<T1> switchList = new ArrayList();
	final HashMap<Long, T1> dpidMap = new HashMap();
    }

    private void registerSwitch(final T1 sw) {
	switchList.add(sw);
	dpidMap.put(((Switch) sw).getSwitchId(), sw);
    }

    private void unregisterSwitch(final T1 sw) {
	dpidMap.remove(((Switch) sw).getSwitchId());
	// TODO: remove ports
	switchList.remove(sw);
    }

    private void registerLink(final T3 link) {
    }

    private void unregisterLink(final T3 link) {
    }

    public boolean initialize() {
	return true;
    }

    public ArrayList<T1> getNeighbours(final T1 sw) {
	return null;
    }

    public T1 getSwitch(final Long dpid) {
	return this.dpidMap.get(dpid);
    }
}
