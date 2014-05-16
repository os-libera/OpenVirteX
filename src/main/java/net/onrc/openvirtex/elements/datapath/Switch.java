/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFVendor;
import org.openflow.util.HexString;

/**
 * The Class Switch.
 *
 * @param <T>
 *            generic type (Port) that is casted in the subclasses
 */

@SuppressWarnings("rawtypes")
public abstract class Switch<T extends Port> implements OVXEventHandler,
        OVXSendMsg {

    public static final String DB_KEY = "switches";
    // Switch channel status
    protected boolean isConnected = false;
    // The channel descriptor
    protected Channel channel = null;
    // The description of OXV stats
    protected OVXDescriptionStatistics desc = null;
    // The switch name (converted from the DPID)
    protected String switchName = null;
    protected Mappable map = null;

    /**
     * The port map. Associate all the port instances with the switch. The port
     * number is the key.
     */
    protected HashMap<Short, T> portMap = null;

    /** The features reply message. */
    protected OFFeaturesReply featuresReply = null;

    /** The switch id (DPID). */
    protected Long switchId = (long) 0;

    /**
     * Instantiates a new switch (should be never used).
     *
     * @param switchId
     *            the switchId (long) that represent the DPID
     * @param map
     *            reference to the OVXMap
     */

    protected Switch(final Long switchId) {
        this.switchId = switchId;
        this.switchName = HexString.toHexString(this.switchId);
        this.portMap = new HashMap<Short, T>();
        this.featuresReply = null;
        this.map = OVXMap.getInstance();
    }

    /**
     * Gets the switch name.
     *
     * @return a user-friendly String that map the switch DPID
     */
    public String getSwitchName() {
        return this.switchName;
    }

    public Mappable getMap() {
        return this.map;
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
     * @param m the new features reply
     */
    public void setFeaturesReply(final OFFeaturesReply m) {
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
     * Returns an unmodifiable copy of the port map.
     */

    public Map<Short, T> getPorts() {
        return Collections.unmodifiableMap(this.portMap);
    }

    /**
     * Gets the port.
     *
     * @param portNumber
     *            the port number
     * @return the port instance
     */
    public T getPort(final Short portNumber) {
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
    public boolean addPort(final T port) {
        if (this.portMap.containsKey(port.getPortNumber())) {
            return false;
        }
        this.portMap.put(port.getPortNumber(), port);
        return true;
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
    public abstract void handleIO(OFMessage msg, Channel channel);

    public abstract void handleRoleIO(OFVendor msg, Channel channel);

    /**
     * Sets the connected.
     *
     * @param isConnected
     *            the new connected
     */
    public void setConnected(final boolean isConnected) {
        this.isConnected = isConnected;
    }

    /**
     * Sets the channel.
     *
     * @param channel
     *            the new channel
     */
    public void setChannel(final Channel channel) {
        this.channel = channel;

    }

    /**
     * Starts up the switch.
     *
     * @return true upon success startup.
     */
    public abstract boolean boot();

    /**
     * Removes the switch from the network representation. Removal may be
     * triggered by an API call (in the case of a OVXSwitch) or disconnection of
     * a switch connected to us (in the case of a PhysicalSwitch).
     */
    public abstract void unregister();

    /**
     * Tear down.
     */
    public abstract void tearDown();

    /**
     * Sets the description stats.
     *
     * @param description
     *            the new description stats
     */
    public void setDescriptionStats(final OVXDescriptionStatistics description) {
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

    public abstract void removeChannel(Channel channel);

}
