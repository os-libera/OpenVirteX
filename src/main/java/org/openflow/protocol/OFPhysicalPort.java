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
/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.openflow.protocol;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Represents ofp_phy_port
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 25, 2010
 */
public class OFPhysicalPort {
    public static int MINIMUM_LENGTH = 48;
    public static int OFP_ETH_ALEN = 6;

    public enum OFPortConfig {
        OFPPC_PORT_DOWN(1 << 0) {
            @Override
            public String toString() {
                return "port-down (0x1)";
            }
        },
        OFPPC_NO_STP(1 << 1) {
            @Override
            public String toString() {
                return "no-stp (0x2)";
            }
        },
        OFPPC_NO_RECV(1 << 2) {
            @Override
            public String toString() {
                return "no-recv (0x4)";
            }
        },
        OFPPC_NO_RECV_STP(1 << 3) {
            @Override
            public String toString() {
                return "no-recv-stp (0x8)";
            }
        },
        OFPPC_NO_FLOOD(1 << 4) {
            @Override
            public String toString() {
                return "no-flood (0x10)";
            }
        },
        OFPPC_NO_FWD(1 << 5) {
            @Override
            public String toString() {
                return "no-fwd (0x20)";
            }
        },
        OFPPC_NO_PACKET_IN(1 << 6) {
            @Override
            public String toString() {
                return "no-pkt-in (0x40)";
            }
        };

        protected int value;

        private OFPortConfig(final int value) {
            this.value = value;
        }

        /**
         * @return the value
         */
        public int getValue() {
            return this.value;
        }
    }

    public enum OFPortState {
        OFPPS_LINK_DOWN(1 << 0) {
            @Override
            public String toString() {
                return "link-down (0x1)";
            }
        },
        OFPPS_STP_LISTEN(0 << 8) {
            @Override
            public String toString() {
                return "listen (0x0)";
            }
        },
        OFPPS_STP_LEARN(1 << 8) {
            @Override
            public String toString() {
                return "learn-no-relay (0x100)";
            }
        },
        OFPPS_STP_FORWARD(2 << 8) {
            @Override
            public String toString() {
                return "forward (0x200)";
            }
        },
        OFPPS_STP_BLOCK(3 << 8) {
            @Override
            public String toString() {
                return "block-broadcast (0x300)";
            }
        },
        OFPPS_STP_MASK(3 << 8) {
            @Override
            public String toString() {
                return "block-broadcast (0x300)";
            }
        };

        protected int value;

        private OFPortState(final int value) {
            this.value = value;
        }

        /**
         * @return the value
         */
        public int getValue() {
            return this.value;
        }
    }

    public enum OFPortFeatures {
        OFPPF_10MB_HD(1 << 0) {
            @Override
            public String toString() {
                return "10mb-hd (0x1)";
            }
        },
        OFPPF_10MB_FD(1 << 1) {
            @Override
            public String toString() {
                return "10mb-fd (0x2)";
            }
        },
        OFPPF_100MB_HD(1 << 2) {
            @Override
            public String toString() {
                return "100mb-hd (0x4)";
            }
        },
        OFPPF_100MB_FD(1 << 3) {
            @Override
            public String toString() {
                return "100mb-fd (0x8)";
            }
        },
        OFPPF_1GB_HD(1 << 4) {
            @Override
            public String toString() {
                return "1gb-hd (0x10)";
            }
        },
        OFPPF_1GB_FD(1 << 5) {
            @Override
            public String toString() {
                return "1gb-fd (0x20)";
            }
        },
        OFPPF_10GB_FD(1 << 6) {
            @Override
            public String toString() {
                return "10gb-fd (0x40)";
            }
        },
        OFPPF_COPPER(1 << 7) {
            @Override
            public String toString() {
                return "copper (0x80)";
            }
        },
        OFPPF_FIBER(1 << 8) {
            @Override
            public String toString() {
                return "fiber (0x100)";
            }
        },
        OFPPF_AUTONEG(1 << 9) {
            @Override
            public String toString() {
                return "autoneg (0x200)";
            }
        },
        OFPPF_PAUSE(1 << 10) {
            @Override
            public String toString() {
                return "pause (0x400)";
            }
        },
        OFPPF_PAUSE_ASYM(1 << 11) {
            @Override
            public String toString() {
                return "pause-asym (0x800)";
            }
        };

        protected int value;

        private OFPortFeatures(final int value) {
            this.value = value;
        }

        /**
         * @return the value
         */
        public int getValue() {
            return this.value;
        }
    }

    protected short portNumber;
    protected byte[] hardwareAddress;
    protected String name;
    protected int config;
    protected int state;
    protected int currentFeatures;
    protected int advertisedFeatures;
    protected int supportedFeatures;
    protected int peerFeatures;

    /**
     * @return the portNumber
     */
    public short getPortNumber() {
        return this.portNumber;
    }

    /**
     * @param portNumber
     *            the portNumber to set
     */
    public void setPortNumber(final short portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * @return the hardwareAddress
     */
    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    /**
     * @param hardwareAddress
     *            the hardwareAddress to set
     */
    public void setHardwareAddress(final byte[] hardwareAddress) {
        if (hardwareAddress.length != OFPhysicalPort.OFP_ETH_ALEN) {
            throw new RuntimeException("Hardware address must have length "
                    + OFPhysicalPort.OFP_ETH_ALEN);
        }
        this.hardwareAddress = hardwareAddress;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the config
     */
    public int getConfig() {
        return this.config;
    }

    /**
     * @param config
     *            the config to set
     */
    public void setConfig(final int config) {
        this.config = config;
    }

    /**
     * @return the state
     */
    public int getState() {
        return this.state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final int state) {
        this.state = state;
    }

    /**
     * @return the currentFeatures
     */
    public int getCurrentFeatures() {
        return this.currentFeatures;
    }

    /**
     * @param currentFeatures
     *            the currentFeatures to set
     */
    public void setCurrentFeatures(final int currentFeatures) {
        this.currentFeatures = currentFeatures;
    }

    /**
     * @return the advertisedFeatures
     */
    public int getAdvertisedFeatures() {
        return this.advertisedFeatures;
    }

    /**
     * @param advertisedFeatures
     *            the advertisedFeatures to set
     */
    public void setAdvertisedFeatures(final int advertisedFeatures) {
        this.advertisedFeatures = advertisedFeatures;
    }

    /**
     * @return the supportedFeatures
     */
    public int getSupportedFeatures() {
        return this.supportedFeatures;
    }

    /**
     * @param supportedFeatures
     *            the supportedFeatures to set
     */
    public void setSupportedFeatures(final int supportedFeatures) {
        this.supportedFeatures = supportedFeatures;
    }

    /**
     * @return the peerFeatures
     */
    public int getPeerFeatures() {
        return this.peerFeatures;
    }

    /**
     * @param peerFeatures
     *            the peerFeatures to set
     */
    public void setPeerFeatures(final int peerFeatures) {
        this.peerFeatures = peerFeatures;
    }

    /**
     * Read this message off the wire from the specified ByteBuffer
     *
     * @param data
     */
    public void readFrom(final ChannelBuffer data) {
        this.portNumber = data.readShort();
        if (this.hardwareAddress == null) {
            this.hardwareAddress = new byte[OFPhysicalPort.OFP_ETH_ALEN];
        }
        data.readBytes(this.hardwareAddress);
        final byte[] name = new byte[16];
        data.readBytes(name);
        // find the first index of 0
        int index = 0;
        for (final byte b : name) {
            if (0 == b) {
                break;
            }
            ++index;
        }
        this.name = new String(Arrays.copyOf(name, index),
                Charset.forName("ascii"));
        this.config = data.readInt();
        this.state = data.readInt();
        this.currentFeatures = data.readInt();
        this.advertisedFeatures = data.readInt();
        this.supportedFeatures = data.readInt();
        this.peerFeatures = data.readInt();
    }

    /**
     * Write this message's binary format to the specified ByteBuffer
     *
     * @param data
     */
    public void writeTo(final ChannelBuffer data) {
        data.writeShort(this.portNumber);
        data.writeBytes(this.hardwareAddress);
        try {
            final byte[] name = this.name.getBytes("ASCII");
            if (name.length < 16) {
                data.writeBytes(name);
                for (int i = name.length; i < 16; ++i) {
                    data.writeByte((byte) 0);
                }
            } else {
                data.writeBytes(name, 0, 15);
                data.writeByte((byte) 0);
            }
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        data.writeInt(this.config);
        data.writeInt(this.state);
        data.writeInt(this.currentFeatures);
        data.writeInt(this.advertisedFeatures);
        data.writeInt(this.supportedFeatures);
        data.writeInt(this.peerFeatures);
    }

    @Override
    public int hashCode() {
        final int prime = 307;
        int result = 1;
        result = prime * result + this.advertisedFeatures;
        result = prime * result + this.config;
        result = prime * result + this.currentFeatures;
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result
                + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + this.peerFeatures;
        result = prime * result + this.portNumber;
        result = prime * result + this.state;
        result = prime * result + this.supportedFeatures;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFPhysicalPort)) {
            return false;
        }
        final OFPhysicalPort other = (OFPhysicalPort) obj;
        if (this.advertisedFeatures != other.advertisedFeatures) {
            return false;
        }
        if (this.config != other.config) {
            return false;
        }
        if (this.currentFeatures != other.currentFeatures) {
            return false;
        }
        if (!Arrays.equals(this.hardwareAddress, other.hardwareAddress)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.peerFeatures != other.peerFeatures) {
            return false;
        }
        if (this.portNumber != other.portNumber) {
            return false;
        }
        if (this.state != other.state) {
            return false;
        }
        if (this.supportedFeatures != other.supportedFeatures) {
            return false;
        }
        return true;
    }
}
