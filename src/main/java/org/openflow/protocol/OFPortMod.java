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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.U16;

/**
 * Represents an ofp_port_mod message
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFPortMod extends OFMessage {
    public static int MINIMUM_LENGTH = 32;

    protected short portNumber;
    protected byte[] hardwareAddress;
    protected int config;
    protected int mask;
    protected int advertise;

    public OFPortMod() {
        super();
        this.type = OFType.PORT_MOD;
        this.length = U16.t(OFPortMod.MINIMUM_LENGTH);
    }

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
     * @return the mask
     */
    public int getMask() {
        return this.mask;
    }

    /**
     * @param mask
     *            the mask to set
     */
    public void setMask(final int mask) {
        this.mask = mask;
    }

    /**
     * @return the advertise
     */
    public int getAdvertise() {
        return this.advertise;
    }

    /**
     * @param advertise
     *            the advertise to set
     */
    public void setAdvertise(final int advertise) {
        this.advertise = advertise;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.portNumber = data.readShort();
        if (this.hardwareAddress == null) {
            this.hardwareAddress = new byte[OFPhysicalPort.OFP_ETH_ALEN];
        }
        data.readBytes(this.hardwareAddress);
        this.config = data.readInt();
        this.mask = data.readInt();
        this.advertise = data.readInt();
        data.readInt(); // pad
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeShort(this.portNumber);
        data.writeBytes(this.hardwareAddress);
        data.writeInt(this.config);
        data.writeInt(this.mask);
        data.writeInt(this.advertise);
        data.writeInt(0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 311;
        int result = super.hashCode();
        result = prime * result + this.advertise;
        result = prime * result + this.config;
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result + this.mask;
        result = prime * result + this.portNumber;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFPortMod)) {
            return false;
        }
        final OFPortMod other = (OFPortMod) obj;
        if (this.advertise != other.advertise) {
            return false;
        }
        if (this.config != other.config) {
            return false;
        }
        if (!Arrays.equals(this.hardwareAddress, other.hardwareAddress)) {
            return false;
        }
        if (this.mask != other.mask) {
            return false;
        }
        if (this.portNumber != other.portNumber) {
            return false;
        }
        return true;
    }
}
