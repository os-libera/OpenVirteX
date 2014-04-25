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
package org.openflow.vendor.nicira;

import org.openflow.protocol.vendor.OFBasicVendorDataType;
import org.openflow.protocol.vendor.OFBasicVendorId;
import org.openflow.protocol.vendor.OFVendorId;

public class OFNiciraVendorExtensions {
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized)
            return;

        // Configure openflowj to be able to parse the role request/reply
        // vendor messages.
        OFBasicVendorId niciraVendorId = new OFBasicVendorId(
                OFNiciraVendorData.NX_VENDOR_ID, 4);
        OFVendorId.registerVendorId(niciraVendorId);
        OFBasicVendorDataType roleRequestVendorData = new OFBasicVendorDataType(
                OFRoleRequestVendorData.NXT_ROLE_REQUEST,
                OFRoleRequestVendorData.getInstantiable());
        niciraVendorId.registerVendorDataType(roleRequestVendorData);
        OFBasicVendorDataType roleReplyVendorData = new OFBasicVendorDataType(
                OFRoleReplyVendorData.NXT_ROLE_REPLY,
                OFRoleReplyVendorData.getInstantiable());
        niciraVendorId.registerVendorDataType(roleReplyVendorData);

        initialized = true;
    }
}
