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

import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.util.HexString;

/**
 * The Class Switch.
 * 
 * @param <T>
 *            generic type (Port) that is casted in the subclasses
 */
public abstract class Switch<T extends Port> implements OVXEventHandler,
	OVXSendMsg {

    /** Switch channel status. */
    protected boolean isConnected = false;

    /** The channel descriptor */
    protected Channel channel = null;

    /** The description of OXV stats */
    protected OVXDescriptionStatistics desc = null;

    /** The switch name (converted from the DPID). */
    protected String switchName = null;

    /**
     * The port map. Associate all the port instances with the switch. The port
     * number is the key.
     */
    protected HashMap<Short, T> portMap = null;

    /** The features reply message. */
    protected OFFeaturesReply featuresReply = null;

    /** The switch id (DPID). */
    protected Long switchId = (long) 0;

    /** The log. */
    private Logger log = LogManager.getLogger(this.getClass().getName());

    /**
     * Instantiates a new switch (should be never used).
     */
    protected Switch() {
	this.switchName = HexString.toHexString(switchId);
	this.portMap = new HashMap<Short, T>();
	this.featuresReply = null;
    }

    /**
     * Instantiates a new switch (should be never used).
     * 
     * @param switchId
     *            the switchId (long) that represent the DPID
     * @param map
     *            reference to the OVXMap
     */

    protected Switch(final Long switchId) {
	super();
	this.switchName = HexString.toHexString(switchId);
	this.switchId = switchId;
	this.portMap = new HashMap<Short, T>();
	this.featuresReply = null;
    }

    /**
     * Gets the switch name.
     * 
     * @return a user-friendly String that map the switch DPID
     */
    public String getSwitchName() {
	return this.switchName;
    }

    /**
     * Gets the switch info.
     * 
     * @return the switch info
     */
    public OFFeaturesReply getFeaturesReply() {
	return this.featuresReply;
    }

    /**
     * Sets the features reply.
     * 
     * @param the
     *            new features reply
     */
    public void setFeaturesReply(OFFeaturesReply m) {
	this.featuresReply = m;
    }

    /**
     * Gets the switch id.
     * 
     * @return the switch id
     */
    public Long getSwitchId() {
	return this.switchId;
    }

    /**
     * Sets the switch id.
     * 
     * @param switchId
     *            the switch id (DPID)
     * @return true, if successful
     */
    public abstract boolean setSwitchId(final Long switchId);

    /**
     * Gets the port.
     * 
     * @param portNumber
     *            the port number
     * @return the port instance
     */
    public T getPort(Short portNumber) {
	return this.portMap.get(portNumber);
    };

    /**
     * Adds the port. If the port is already present then no action is
     * performed.
     * 
     * @param port
     *            the port instance
     * @return true, if successful
     */
    public boolean addPort(T port) {
	if (this.portMap.containsKey(port.getPortNumber()))
	    return false;
	this.portMap.put(port.getPortNumber(), port);
	return true;
    }

    /**
     * Update port. Adds the port only if the port is already present.
     * 
     * @param port
     *            the port instance
     * @return true, if updated
     */
    public boolean updatePort(T port) {
	if (this.portMap.containsKey(port.getPortNumber())) {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
	return false;
    }

    /**
     * Removes the port.
     * 
     * @param portNumber
     *            the port number
     * @return true, if successful
     */
    public boolean removePort(Short portNumber) {
	if (this.portMap.containsKey(portNumber)) {
	    this.portMap.remove(portNumber);
	    return true;
	}
	return false;
    };

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.core.io.OVXEventHandler#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public abstract void handleIO(OFMessage msgs);

    /**
     * Sets the connected.
     * 
     * @param isConnected
     *            the new connected
     */
    public void setConnected(boolean isConnected) {
	this.isConnected = isConnected;
    }

    /**
     * Sets the channel.
     * 
     * @param channel
     *            the new channel
     */
    public void setChannel(Channel channel) {
	this.channel = channel;

    }

    /**
     * Tear down.
     */
    public abstract void tearDown();

    /**
     * Inits the switch.
     */
    public abstract void init();

    /**
     * Sets the description stats.
     * 
     * @param description
     *            the new description stats
     */
    public void setDescriptionStats(OVXDescriptionStatistics description) {
	this.desc = description;

    }

    @Override
    public String getName() {
	return this.switchName + ":" + this.switchId;
    }

    @Override
    public String toString() {
	return "SWITCH:\n- switchId: " + this.switchId + "\n- switchName: "
		+ this.switchName + "\n- isConnected: " + this.isConnected;
    }
}
