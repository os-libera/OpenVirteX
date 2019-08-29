/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.statistics.OVXDescStatsReply;
import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.util.HexString;

import java.util.*;

public abstract class Switch<T extends Port> implements OVXEventHandler,
        OVXSendMsg {

    public static final String DB_KEY = "switches";
    // Switch channel status
    protected boolean isConnected = false;
    // The channel descriptor
    protected Channel channel = null;
    // The description of OXV stats
    //protected OVXDescriptionStatistics desc = null;
    protected OVXDescStatsReply desc = null;
    // The switch name (converted from the DPID)
    protected String switchName = null;
    protected Mappable map = null;

    /**
     * The port map. Associate all the port instances with the switch. The port
     * number is the key.
     */
    protected HashMap<Short, T> portMap = null;

    /** The features reply message for OF_1.0. */
    protected OFFeaturesReply featuresReply = null;


    protected List<OFPortDesc> ports = new ArrayList<>();

    /** The switch id (DPID). */
    protected Long switchId = (long) 0;

    /** OpenFlow Version */
    protected OFVersion ofVersion;
    protected OFFactory ofFactory;

    /** The port_desc reply message for OF_1.3. */
    protected List<OFPortDescStatsReply> portDescStatsReplies = new ArrayList<>();
    protected OFPortDescStatsReply portDescStatsReply = null;



    /** for port description for OF_1.3*/
    public void setPortDescReply(OFPortDescStatsReply m) {
        this.portDescStatsReply = m;
    }

    public void setPortDescReplies(List<OFPortDescStatsReply> portDescReplies) {
        this.portDescStatsReplies.addAll(portDescReplies);
    }

    public OFPortDescStatsReply getPortDescStatsReply() {
        return this.portDescStatsReply;
    }


    /**
     * Instantiates a new switch (should be never used).
     *
     * @param switchId
     *            the switchId (long) that represent the DPID
     *
     */

    protected Switch(final Long switchId) {
        this.switchId = switchId;
        this.switchName = HexString.toHexString(this.switchId);
        this.portMap = new HashMap<Short, T>();

        this.featuresReply = null;
        this.portDescStatsReply = null;

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
     * Sets the OFPortDesc
     *
     * @param portDescEntries the new features reply
     */

    public void setPortDescEntries(List<OFPortDesc> portDescEntries) {
        this.ports.addAll(portDescEntries);
    }

    public List<OFPortDesc> getPortDescEntries() {
        return this.ports;
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
    public abstract void handleIO(OVXMessage msg, Channel channel);

    public abstract void handleRoleIO(OVXMessage msg, Channel channel);

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

    /*
        Set OpenFlow Version
     */
    public void setOfVersion(OFVersion ofv) {
        this.ofVersion = ofv;
        this.ofFactory = OFFactories.getFactory(ofv);
    }

    /*
        Get OpenFlow Version
     */
    public OFVersion getOfVersion() { return this.ofVersion; }

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
    public void setDescriptionStats(final OVXDescStatsReply description) {
        this.desc = description;

    }

    @Override
    public String getName() {
        return this.switchName + "--" + this.switchId;
    }

    @Override
    public String toString() {
        return "SWITCH:\n- switchId: " + this.switchId + "\n- switchName: "
                + this.switchName + "\n- isConnected: " + this.isConnected;
    }

    public abstract void removeChannel(Channel channel);
}
