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
package net.onrc.openvirtex.elements.port;

import java.util.*;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.Link;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.PortSpeed;

/**
 * The Class Port.
 *
 * @param <T1>
 *            The Generic Switch type
 * @param <T2>
 *            The Generic Link type
 */

@SuppressWarnings("rawtypes")

public class Port<T1 extends Switch, T2 extends Link>
        implements Persistable {

    /**
     * Database keyword for ports.
     */
    public static final String DB_KEY = "ports";

    protected MacAddress mac;
    protected Boolean isEdge;
    protected T1 parentSwitch;
    protected LinkPair<T2> portLink;

    // TODO: duplexing/speed on port/link???

    //org.projectfloodlight.openflow
    protected OFPortDesc ofPort;

    protected short portNumber;
    protected byte[] hardwareAddress;
    protected String name;
    protected Set<OFPortConfig> config = null;
    protected Set<OFPortState> state = null;
    protected Set<OFPortFeatures> currentFeatures = null;
    protected Set<OFPortFeatures> advertisedFeatures = null;
    protected Set<OFPortFeatures> supportedFeatures = null;
    protected Set<OFPortFeatures> peerFeatures = null;

    /**
     * Instantiates a new port.
     *
     * @param ofPort the OpenFlow physical port
     */
    protected Port(final OFPortDesc ofPort) {
        this.ofPort = ofPort.createBuilder().build();

        this.portNumber = ofPort.getPortNo().getShortPortNumber();
        this.hardwareAddress = ofPort.getHwAddr().getBytes();
        this.name = ofPort.getName();
        this.mac = ofPort.getHwAddr();
        this.isEdge = true;
        this.parentSwitch = null;
        this.portLink = null;

        this.config = new HashSet<>();
        this.config.addAll(ofPort.getConfig());

        this.state = new HashSet<>();
        this.state.addAll(ofPort.getState());

        this.currentFeatures = new HashSet<>();
        this.currentFeatures.addAll(ofPort.getCurr());

        this.advertisedFeatures = new HashSet<>();
        this.advertisedFeatures.addAll(ofPort.getAdvertised());

        this.supportedFeatures = new HashSet<>();
        this.supportedFeatures.addAll(ofPort.getSupported());

        this.peerFeatures = new HashSet<>();
        this.peerFeatures.addAll(ofPort.getPeer());

        if(this.hardwareAddress == null) {
            this.hardwareAddress = new byte[] {(byte) 0xDE, (byte) 0xAD,
                    (byte) 0xBE, (byte) 0xEF, (byte) 0xCA, (byte) 0xFE};
        }
        this.mac = MacAddress.of(this.hardwareAddress);
        this.isEdge = true;
        this.parentSwitch = null;
        this.portLink = null;
    }

     /*
        Gets short PortNo
     */
    public short getPortNumber() {
        return this.ofPort.getPortNo().getShortPortNumber();
    }

    /*
        Gets the PortDesc
     */
    public OFPortDesc getOfPort() { return this.ofPort; }

    /**
     * Gets the checks if is edge.
     *
     * @return the checks if is edge
     */
    public Boolean isEdge() {
        return this.isEdge;
    }

    /**
     * Sets the checks if is edge.
     *
     * @param isEdge
     *            the new checks if is edge
     */
    public void setEdge(final Boolean isEdge) {
        this.isEdge = isEdge;
    }

    public T1 getParentSwitch() {
        return this.parentSwitch;
    }

    /**
     * Set the link connected to this port.
     *
     * @param link
     */
    public void setInLink(T2 link) {
        if (this.portLink == null) {
            this.portLink = new LinkPair<T2>();
        }
        this.portLink.setInLink(link);
    }

    /**
     * Set the link connected to this port.
     *
     * @param link
     */
    public void setOutLink(T2 link) {
        if (this.portLink == null) {
            this.portLink = new LinkPair<T2>();
        }
        this.portLink.setOutLink(link);
    }

    /**
     * @return The physical link connected to this port
     */
    public LinkPair<T2> getLink() {
        return this.portLink;
    }

    /**
     *
     * @return the highest nominal throughput currently exposed by the port
     */
    public Integer getCurrentThroughput() {

        PortSpeed portSpeed = PortSpeed.SPEED_NONE;

        for (OFPortFeatures feat : this.currentFeatures) {
            portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
        }

        return new Integer((int)(portSpeed.getSpeedBps() / 1000L / 1000L));

        //PortFeatures feature = new PortFeatures(this.currentFeatures);
        //return feature.getHighestThroughput();
    }

    @Override
    public String toString() {
        return this.ofPort.toString() + "\n isEdge = " + this.isEdge;
        /*return "PORT:\n- portNumber: " + this.portNumber + "\n- parentSwitch: "
                + this.getParentSwitch().getSwitchName()
                + "\n- hardwareAddress: "
                + MACAddress.valueOf(this.hardwareAddress).toString()
                + "\n- config: " + this.config + "\n- state: " + this.state
                + "\n- currentFeatures: " + this.currentFeatures
                + "\n- advertisedFeatures: " + this.advertisedFeatures
                + "\n- supportedFeatures: " + this.supportedFeatures
                + "\n- peerFeatures: " + this.peerFeatures + "\n- isEdge: "
                + this.isEdge;*/
    }

    /*
     * should transient features of the Port be taken into account by
     * hashCode()? They prevent ports from being fetched from maps once their
     * status changes.
     */
    @Override
    public int hashCode() {
        final int prime = 307;
        int result = 1;
        result = prime * result + this.advertisedFeatures.hashCode();
        result = prime * result + this.config.hashCode();
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result
                + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + this.portNumber;
        result = prime * result + this.parentSwitch.hashCode();
        return result;
        //return this.ofPort.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
       if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Port)) {
            return false;
        }
        Port other = (Port) obj;
        if (parentSwitch == null) {
            if (other.parentSwitch != null) {
                return false;
            }
        } else if (!parentSwitch.equals(other.parentSwitch)) {
            return false;
        }
        return true;
    }

    @Override
    public Map<String, Object> getDBIndex() {
        return null;
    }

    @Override
    public String getDBKey() {
        return null;
    }

    @Override
    public String getDBName() {
        return null;
    }

    @Override
    public Map<String, Object> getDBObject() {
        Map<String, Object> dbObject = new HashMap<String, Object>();
        dbObject.put(TenantHandler.DPID, this.parentSwitch.getSwitchId());
        dbObject.put(TenantHandler.PORT, this.portNumber);
        return dbObject;
    }

    public DPIDandPort toDPIDandPort() {
        return new DPIDandPort(this.parentSwitch.getSwitchId(), this.portNumber);
    }
}
