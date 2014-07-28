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
package net.onrc.openvirtex.elements.address;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;

/**
 * Tests for IP addresses.
 */
public class IPTest extends TestCase {

    private static final String DEFAULT = "10.0.0.1";

    public IPTest(final String name) {
        super(name);
    }

    private Mappable map = null;

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(IPTest.class);
    }

    public void testPhysicalIP() {
        final PhysicalIPAddress ip = new PhysicalIPAddress(IPTest.DEFAULT);

        Assert.assertEquals("PhysicalIPAddress[" + IPTest.DEFAULT + "]",
                ip.toString());
    }

    public void testVirtualIP() {
        final OVXIPAddress ip = new OVXIPAddress(IPTest.DEFAULT, 0);

        Assert.assertEquals("OVXIPAddress[" + IPTest.DEFAULT + "]",
                ip.toString());
    }

    public void testgetPhysicalIp() {
        final OVXIPAddress virtualip = new OVXIPAddress(IPTest.DEFAULT, 0);
        this.map.addIP(new PhysicalIPAddress("1.0.0.1"), virtualip);

        final Integer physicalip = IPMapper.getPhysicalIp(0, virtualip.getIp());

        Assert.assertEquals(
                (Integer) new PhysicalIPAddress("1.0.0.1").getIp(),
                physicalip);
    }

    public void testgetPhysicalIpVIP0ShouldReturn0() {
        final Integer physicalip = IPMapper.getPhysicalIp(0, 0);

        Assert.assertEquals((Integer) 0, physicalip);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.map = OVXMap.getInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
