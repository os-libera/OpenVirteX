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

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.protocol.factory.OFVendorDataFactory;
import org.openflow.protocol.factory.OFVendorDataFactoryAware;
import org.openflow.protocol.vendor.OFVendorData;
import org.openflow.util.U16;

/**
 * Represents ofp_vendor_header
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFVendor extends OFMessage implements OFVendorDataFactoryAware {
    public static int MINIMUM_LENGTH = 12;

    protected int vendor;
    protected OFVendorData vendorData;
    protected OFVendorDataFactory vendorDataFactory;

    public OFVendor() {
        super();
        this.type = OFType.VENDOR;
        this.length = U16.t(OFVendor.MINIMUM_LENGTH);
    }

    /**
     * @return the vendor
     */
    public int getVendor() {
        return this.vendor;
    }

    /**
     * @param vendor
     *            the vendor to set
     */
    public void setVendor(final int vendor) {
        this.vendor = vendor;
    }

    /**
     * @return the data
     */
    public OFVendorData getVendorData() {
        return this.vendorData;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setVendorData(final OFVendorData vendorData) {
        this.vendorData = vendorData;
    }

    @Override
    public void setVendorDataFactory(final OFVendorDataFactory vendorDataFactory) {
        this.vendorDataFactory = vendorDataFactory;
    }

    @Override
    public void readFrom(final ChannelBuffer data) {
        super.readFrom(data);
        this.vendor = data.readInt();
        if (this.vendorDataFactory == null) {
            throw new RuntimeException("OFVendorDataFactory not set");
        }

        this.vendorData = this.vendorDataFactory.parseVendorData(this.vendor,
                data, super.getLengthU() - OFVendor.MINIMUM_LENGTH);
    }

    @Override
    public void writeTo(final ChannelBuffer data) {
        super.writeTo(data);
        data.writeInt(this.vendor);
        if (this.vendorData != null) {
            this.vendorData.writeTo(data);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 337;
        int result = super.hashCode();
        result = prime * result + this.vendor;
        if (this.vendorData != null) {
            result = prime * result + this.vendorData.hashCode();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final OFVendor other = (OFVendor) obj;
        if (this.vendor != other.vendor) {
            return false;
        }
        if (this.vendorData == null) {
            if (other.vendorData != null) {
                return false;
            }
        } else if (!this.vendorData.equals(other.vendorData)) {
            return false;
        }
        return true;
    }
}
