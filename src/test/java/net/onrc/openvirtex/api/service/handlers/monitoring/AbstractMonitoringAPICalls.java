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
package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.HashMap;

import junit.framework.TestCase;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AbstractMonitoringAPICalls extends TestCase{

    public JSONRPC2Response getVirtualAddressMapping(final int tenantId) {
        final GetVirtualAddressMapping gvam = new GetVirtualAddressMapping();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
            }
        };

        return gvam.process(request);

    }

    public void testPassing() {
        /* Make JUnit happy */
        /* http://junit.sourceforge.net/doc/faq/faq.htm#running_11 */
    }

}
