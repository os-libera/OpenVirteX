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
 *    Copyright 2013, Big Switch Networks, Inc.
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

package org.openflow.protocol.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton registry object that holds a mapping from vendor ids to
 * vendor-specific mapping factories. Threadsafe.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
public class OFVendorActionRegistry {
    private static class InstanceHolder {
        private final static OFVendorActionRegistry instance = new OFVendorActionRegistry();
    }

    public static OFVendorActionRegistry getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<Integer, OFVendorActionFactory> vendorActionFactories;

    public OFVendorActionRegistry() {
        this.vendorActionFactories = new ConcurrentHashMap<Integer, OFVendorActionFactory>();
    }

    public OFVendorActionFactory register(final int vendorId,
            final OFVendorActionFactory factory) {
        return this.vendorActionFactories.put(vendorId, factory);
    }

    public OFVendorActionFactory get(final int vendorId) {
        return this.vendorActionFactories.get(vendorId);
    }

}
