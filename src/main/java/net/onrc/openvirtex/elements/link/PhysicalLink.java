/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.link;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort.OFPortState;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The Class PhysicalLink.
 *
 */
public class PhysicalLink extends Link<PhysicalPort, PhysicalSwitch> implements
        Persistable, Comparable<PhysicalLink>, Component {

    /**
     * The FSM describing PhysicalLink state. A Link's state is dependent on the
     * state of its ports:
     * 
     * <ul>
     * <li>Both ports ACTIVE: Link is ACTIVE</li>
     * <li>One or both ports INACTIVE: Link is INACTIVE</li>
     * <li>One or both ports STOPPED: Link is STOPPED</li>
     * </ul>
     * 
     * This behavior is more clear from the PhysicalPort FSM, which drives this
     * one. The Link's state primarily affects the OFPortState of one end if the
     * other end causes the Link's state to change.
     */
    enum LinkState {
        INIT {
            @Override
            protected void register(final PhysicalLink link) {
                PhysicalLink.log.debug("registering link {}-{}",
                        link.srcPort.toAP(), link.dstPort.toAP());
                link.srcPort.setOutLink(link);
                link.dstPort.setInLink(link);
                link.state = LinkState.INACTIVE;
            }
        },
        INACTIVE {
            @Override
            protected boolean boot(final PhysicalLink link) {
                PhysicalLink.log.info("enabling link {}-{}",
                        link.srcPort.toAP(), link.dstPort.toAP());
                /* set state to link_up */
                final int linkup = ~OFPortState.OFPPS_LINK_DOWN.getValue();
                link.srcPort.setState(link.srcPort.getState() & linkup);
                link.srcPort.setEdge(false);
                link.dstPort.setState(link.dstPort.getState() & linkup);
                link.dstPort.setEdge(false);
                link.state = LinkState.ACTIVE;
                return true;
            }

            @Override
            protected void unregister(final PhysicalLink link) {
                /* remove oneself from end-points' LinkPairs and OVXMap. */
                PhysicalLink.log.debug("unregistering link {}-{}",
                        link.srcPort.toAP(), link.dstPort.toAP());
                link.state = LinkState.STOPPED;

                LinkState.setEndStates(link,
                        OFPortState.OFPPS_LINK_DOWN.getValue());
                link.srcPort.setOutLink(null);
                link.dstPort.setInLink(null);
                link.getSrcSwitch().getMap().removePhysicalLink(link);
            }
        },
        ACTIVE {
            @Override
            protected boolean teardown(final PhysicalLink link) {
                PhysicalLink.log.info("disabling link {}-{}",
                        link.srcPort.toAP(), link.dstPort.toAP());
                link.state = LinkState.INACTIVE;

                /* set state to link_down for end-points */
                LinkState.setEndStates(link,
                        OFPortState.OFPPS_LINK_DOWN.getValue());
                link.srcPort.setEdge(true);
                link.dstPort.setEdge(true);
                return true;
            }
        },
        STOPPED;

        /**
         * Initializes a PhysicalLink's FSM state from INIT to INACTIVE
         *
         * @param link
         *            the PhysicalLink to initialize
         */
        protected void register(final PhysicalLink link) {
            PhysicalLink.log.debug("Cannot register link while status={}",
                    link.state);
        }

        /**
         * Sets a PhysicalLink's FSM state to ACTIVE
         *
         * @param link
         *            the PhysicalLink to enable
         */
        protected boolean boot(final PhysicalLink link) {
            PhysicalLink.log.debug("Cannot boot link while status={}",
                    link.state);
            return false;
        }

        /**
         * Sets a PhysicalLink's FSM state to INACTIVE
         *
         * @param link
         *            the PhysicalLink to disable
         */
        protected boolean teardown(final PhysicalLink link) {
            PhysicalLink.log.debug("Cannot teardown link while status={}",
                    link.state);
            return false;
        }

        /**
         * Sets a PhysicalLink's FSM state to STOPPED, permanently removing it.
         *
         * @param link
         *            the PhysicalLink to unregister
         */
        protected void unregister(final PhysicalLink link) {
            PhysicalLink.log.debug("Cannot unregister link while status={}",
                    link.state);
        }

        /**
         * Sets the OFPortState values of a PhysicalLink's end-points.
         *
         * @param link
         *            the PhysicalLink making a state change
         * @param nstate
         */
        private static void setEndStates(final PhysicalLink link,
                final int nstate) {
            link.srcPort.setState(link.srcPort.getState() | nstate);
            link.dstPort.setState(link.dstPort.getState() | nstate);
        }
    }

    private static Logger        log     = LogManager
                                                 .getLogger(PhysicalLink.class
                                                         .getName());
    /** Counter for link ID generation */
    private static AtomicInteger linkIds = new AtomicInteger(0);

    @SerializedName("linkId")
    @Expose
    private Integer              linkId  = null;
    private LinkState            state;

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
        this.linkId = PhysicalLink.linkIds.getAndIncrement();
        this.state = LinkState.INIT;
        this.register();
    }

    public Integer getLinkId() {
        return this.linkId;
    }

    @Override
    public void unregister() {
        this.state.unregister(this);
    }

    @Override
    public Map<String, Object> getDBObject() {
        final Map<String, Object> dbObject = super.getDBObject();
        dbObject.put(TenantHandler.LINK, this.linkId);
        return dbObject;
    }

    public void setLinkId(final Integer id) {
        this.linkId = id;
    }

    @Override
    public int compareTo(final PhysicalLink o) {
        final Long sum1 = this.getSrcSwitch().getSwitchId()
                + this.getSrcPort().getPortNumber();
        final Long sum2 = o.getSrcSwitch().getSwitchId()
                + o.getSrcPort().getPortNumber();
        if (sum1 == sum2) {
            return (int) (this.getSrcSwitch().getSwitchId() - o.getSrcSwitch()
                    .getSwitchId());
        } else {
            return (int) (sum1 - sum2);
        }
    }

    @Override
    public void register() {
        this.state.register(this);
    }

    @Override
    public boolean boot() {
        return this.state.boot(this);
    }

    @Override
    public boolean tearDown() {
        return this.state.teardown(this);
    }

}
