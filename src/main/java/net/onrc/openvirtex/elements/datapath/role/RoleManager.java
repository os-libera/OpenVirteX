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
package net.onrc.openvirtex.elements.datapath.role;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.exceptions.UnknownRoleException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFMessage;
import org.openflow.vendor.nicira.OFRoleVendorData;

public class RoleManager {

    private static Logger                                 log = LogManager
                                                                      .getLogger(RoleManager.class
                                                                              .getName());
    private HashMap<Channel, Role>                        state;
    private final AtomicReference<HashMap<Channel, Role>> currentState;
    private Channel                                       currentMaster;

    public static enum Role {
        EQUAL(OFRoleVendorData.NX_ROLE_OTHER), MASTER(
                OFRoleVendorData.NX_ROLE_MASTER), SLAVE(
                OFRoleVendorData.NX_ROLE_SLAVE);

        private final int nxRole;

        private Role(final int nxRole) {
            this.nxRole = nxRole;
        }

        private static Map<Integer, Role> nxRoleToEnum = new HashMap<Integer, Role>();
        static {
            for (final Role r : Role.values()) {
                Role.nxRoleToEnum.put(r.toNxRole(), r);
            }
        }

        public int toNxRole() {
            return this.nxRole;
        }

        // Return the enum representing the given nxRole or null if no
        // such role exists
        public static Role fromNxRole(final int nxRole) {
            return Role.nxRoleToEnum.get(nxRole);
        }

    };

    public RoleManager() {
        this.state = new HashMap<Channel, Role>();
        this.currentState = new AtomicReference<HashMap<Channel, Role>>(
                this.state);
    }

    private HashMap<Channel, Role> getState() {
        return new HashMap<>(this.currentState.get());
    }

    private void setState() {
        this.currentState.set(this.state);
    }

    public synchronized void addController(final Channel chan) {
        if (chan == null) {
            return;
        }
        this.state = this.getState();
        this.state.put(chan, Role.EQUAL);
        this.setState();
    }

    public synchronized void setRole(final Channel channel, final Role role)
            throws IllegalArgumentException, UnknownRoleException {
        if (!this.currentState.get().containsKey(channel)) {
            throw new IllegalArgumentException("Unknown controller "
                    + channel.getRemoteAddress());
        }
        this.state = this.getState();
        RoleManager.log.info("Setting controller {} to role {}",
                channel.getRemoteAddress(), role);
        switch (role) {
            case MASTER:
                if (channel == this.currentMaster) {
                    this.state.put(channel, Role.MASTER);
                    break;
                }
                this.state.put(this.currentMaster, Role.SLAVE);
                this.state.put(channel, Role.MASTER);
                this.currentMaster = channel;
                break;
            case SLAVE:
                if (channel == this.currentMaster) {
                    this.state.put(channel, Role.SLAVE);
                    this.currentMaster = null;
                    break;
                }
                this.state.put(channel, Role.SLAVE);
                break;
            case EQUAL:
                if (channel == this.currentMaster) {
                    this.state.put(channel, Role.EQUAL);
                    this.currentMaster = null;
                    break;
                }
                this.state.put(channel, Role.EQUAL);
                break;
            default:
                throw new UnknownRoleException("Unkown role : " + role);

        }
        this.setState();

    }

    public boolean canSend(final Channel channel, final OFMessage m) {
        final Role r = this.currentState.get().get(channel);
        if (r == Role.MASTER || r == Role.EQUAL) {
            return true;
        }
        switch (m.getType()) {
            case GET_CONFIG_REQUEST:
            case QUEUE_GET_CONFIG_REQUEST:
            case PORT_STATUS:
            case STATS_REQUEST:
                return true;
            default:
                return false;
        }
    }

    public boolean canReceive(final Channel channel, final OFMessage m) {
        final Role r = this.currentState.get().get(channel);
        if (r == Role.MASTER || r == Role.EQUAL) {
            return true;
        }
        switch (m.getType()) {
            case GET_CONFIG_REPLY:
            case QUEUE_GET_CONFIG_REPLY:
            case PORT_STATUS:
            case STATS_REPLY:
                return true;
            default:
                return false;
        }
    }

    public Role getRole(final Channel channel) {
        return this.currentState.get().get(channel);
    }

    private void checkAndSend(final Channel c, final OFMessage m) {
        if (this.canReceive(c, m)) {
            if (c != null && c.isOpen()) {
                c.write(Collections.singletonList(m));
            }
        }

    }

    public void sendMsg(final OFMessage msg, final Channel c) {
        if (c != null) {
            this.checkAndSend(c, msg);
        } else {
            final Map<Channel, Role> readOnly = Collections
                    .unmodifiableMap(this.currentState.get());
            for (final Channel chan : readOnly.keySet()) {
                if (chan == null) {
                    continue;
                }
                this.checkAndSend(chan, msg);
            }
        }
    }

    public synchronized void removeChannel(final Channel channel) {
        this.state = this.getState();
        this.state.remove(channel);
        this.setState();
    }

    public synchronized void shutDown() {
        this.state = this.getState();
        for (final Channel c : this.state.keySet()) {
            if (c != null && c.isConnected()) {
                c.close();
            }
        }
        this.state.clear();
        this.setState();
    }

    @Override
    public String toString() {
        return this.currentState.get().toString();
    }
}
