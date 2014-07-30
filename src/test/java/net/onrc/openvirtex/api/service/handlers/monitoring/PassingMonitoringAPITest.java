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

import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestSuite;
//import net.onrc.openvirtex.core.OpenVirteXController;
//import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Tests for the API calls.
 */
public class PassingMonitoringAPITest extends AbstractMonitoringAPICalls {

//    private static final OpenVirteXController CTRL =
//            new OpenVirteXController(new CmdLineSettings());
    private static final String TENANT1_VIP = "10.0.0.1";
    private static final String TENANT1_PIP = "1.0.0.1";
    private static final String TENANT2_VIP = "20.0.0.1";
    private static final String TENANT2_PIP = "2.0.0.1";

    private Mappable map = null;

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(PassingMonitoringAPITest.class);
    }

    /**
     * Test GetVirtualAddressMapping.
     */
    @SuppressWarnings("unchecked")
    public void testGetVirtualAddressMapping() {
        map.addIP(new PhysicalIPAddress(TENANT1_PIP),
                new OVXIPAddress(TENANT1_VIP, 1));
        map.addIP(new PhysicalIPAddress(TENANT2_PIP),
                new OVXIPAddress(TENANT2_VIP, 2));

        final JSONRPC2Response resp1 = super.getVirtualAddressMapping(1);
        final JSONRPC2Response resp2 = super.getVirtualAddressMapping(2);

        // tenant 1
        Assert.assertNull("GetVirtualAddressMapping should not return null",
                resp1.getError());

        Assert.assertTrue("GetVirtualAddressMapping has incorrect return type",
                resp1.getResult() instanceof Map<?, ?>);

        Map<String, Object> result1 = (Map<String, Object>) resp1.getResult();
        Assert.assertEquals(TENANT1_PIP, result1.get(TENANT1_VIP));

        // tenant 2
        Assert.assertNull("GetVirtualAddressMapping should not return null",
                resp2.getError());

        Assert.assertTrue("GetVirtualAddressMapping has incorrect return type",
                resp2.getResult() instanceof Map<?, ?>);

        Map<String, Object> result2 = (Map<String, Object>) resp2.getResult();

        Assert.assertEquals(TENANT2_PIP, result2.get(TENANT2_VIP));
    }

    @Override
    protected void setUp() throws Exception {
        this.map = OVXMap.getInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        OVXMap.reset();
    }

}
