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

import java.util.HashMap;

import net.onrc.openvirtex.elements.OVXMap;

import org.openflow.protocol.OFFeaturesReply;
import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFMessage;

/**
 * The Class Switch.
 * 
 * @param <T1>
 *            the generic type
 * @author gerola
 */
public abstract class Switch<T1> implements OVXEventHandler, OVXSendMsg {    
    
    protected boolean isConnected = false;

    protected Channel channel = null;

    protected OVXDescriptionStatistics desc;

    /** The switch name. */
    private String                     switchName;

    /** The port map. */
    protected final HashMap<Short, T1> portMap;

    /** The switch info. */
    protected OFFeaturesReply            featuresReply;

    /** The switch id. */
    private long                       switchId;

    /** The map. */
    private final OVXMap               map;

    /**
     * Instantiates a new switch.
     */
    public Switch() {
	super();
	this.switchName = "";
	this.switchId = 0;
	this.map = null;
	this.portMap = new HashMap<Short, T1>();
	this.featuresReply = null;
    }

    /**
     * Instantiates a new switch.
     * 
     * @param switchName
     *            the switch name
     * @param switchId
     *            the switch id
     * @param map
     *            the map
     */

    public Switch(final String switchName, final long switchId, final OVXMap map) {
	super();
	this.switchName = switchName;
	this.switchId = switchId;
	this.map = map;
	this.portMap = new HashMap<Short, T1>();
	this.featuresReply = null;
    }

    /**
     * Gets the switch name.
     * 
     * @return the switch name
     */
    public String getSwitchName() {
	return this.switchName;
    }

    /**
     * Sets the switch name.
     * 
     * @param switchName
     *            the switch name
     * @return true, if successful
     */
    public boolean setSwitchName(final String switchName) {
	this.switchName = switchName;
	return true;
    }

    /**
     * Gets the switch info.
     * 
     * @return the switch info
     */
    public OFFeaturesReply getFeaturesReply() {
	return this.featuresReply;
    }

    public void setFeaturesReply(OFFeaturesReply m) {
	this.featuresReply = m;
    }
    
    /**
     * Gets the switch id.
     * 
     * @return the switch id
     */
    public long getSwitchId() {
	return this.switchId;
    }

    /**
     * Sets the switch id.
     * 
     * @param switchId
     *            the switch id
     * @return true, if successful
     */
    public boolean setSwitchId(final long switchId) {
	this.switchId = switchId;
	return true;
    }

    /**
     * Gets the port map.
     * 
     * @return the port map
     */
    public HashMap<Short, T1> getPortMap() {
	return new HashMap<Short, T1>(this.portMap);
    }

    /**
     * Gets the port.
     * 
     * @param portNumber
     *            the port number
     * @return the port
     */
    public abstract T1 getPort(short portNumber);

    /**
     * Adds the port.
     * 
     * @param port
     *            the port
     * @return true, if successful
     */
    public abstract boolean addPort(T1 port);

    /**
     * Update port.
     * 
     * @param port
     *            the port
     * @return true, if successful
     */
    public abstract boolean updatePort(T1 port);

    /**
     * Removes the port.
     * 
     * @param portNumber
     *            the port number
     * @return true, if successful
     */
    public abstract boolean removePort(short portNumber);

    /**
     * Initialize.
     * 
     * @return true, if successful
     */
    public abstract boolean initialize();

    @Override
	public abstract void handleIO(OFMessage msgs);

    public void setConnected(boolean isConnected) {
	this.isConnected = isConnected;
    }

    public void setChannel(Channel channel) {
	this.channel  = channel;
	
    }

    public abstract void tearDown();

    public abstract void init();

    public void setDescriptionStats(OVXDescriptionStatistics description) {
	this.desc = description;
	
    }
}
