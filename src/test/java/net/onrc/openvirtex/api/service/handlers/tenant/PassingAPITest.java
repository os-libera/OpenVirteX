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
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestSuite;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import org.openflow.protocol.OFPhysicalPort;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import java.lang.Long;

/**
 * Tests for the API calls.
 */
public class PassingAPITest extends AbstractAPICalls {

    private static final OpenVirteXController CTRL =
            new OpenVirteXController(new CmdLineSettings());

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(PassingAPITest.class);
    }

    /**
     * Tests whether a create network call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateNetPass() {

        final JSONRPC2Response resp = super.createNetwork();

        Assert.assertNull("CreateOVXNetwork should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXNetwork has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));
    }

    /**
     * Test whether the creation of a host actually succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateSingleSwitch() {
        final PhysicalSwitch sw = new PhysicalSwitch(1);
        PhysicalNetwork.getInstance().addSwitch(sw);

        super.createNetwork();
        final JSONRPC2Response resp = super.createSwitch(1,
                Collections.singletonList((long)1));

        Assert.assertNull("CreateOVXSwitch should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXSwitch has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((long) 46200400562356225L,
                result.get(TenantHandler.VDPID));
    }

    /**
     * Test whether the creation of a host actually succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreateBigSwitch() {
        final PhysicalSwitch sw1 = new PhysicalSwitch(1);
        final PhysicalSwitch sw2 = new PhysicalSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        PhysicalNetwork.getInstance().createLink(p1, p2);
        PhysicalNetwork.getInstance().createLink(p2, p1);
        super.createNetwork();
        final List<Long> l = new LinkedList<>();
        l.add((long)1);
        l.add((long)2);
        final JSONRPC2Response resp = super.createSwitch(1, l);

        Assert.assertNull("CreateOVXSwitch should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXSwitch has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((long) 46200400562356225L,
                result.get(TenantHandler.VDPID));
    }

    /**
     * Tests whether a create port call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testCreatePort() {
        final TestSwitch sw = new TestSwitch(1);
        PhysicalNetwork.getInstance().addSwitch(sw);
        final PhysicalPort port = new PhysicalPort(new OFPhysicalPort(), sw,
                true);
        port.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        port.setPortNumber((short) 1);
        sw.addPort(port);

        super.createNetwork();
        super.createSwitch(1, Collections.singletonList((long)1));

        final JSONRPC2Response resp = super.createPort(1, (long) 1, (short) 1);

        Assert.assertNull("CreateOVXPort should not return null",
                resp.getError());

        Assert.assertTrue("CreateOVXPort has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals((short) 1, result.get(TenantHandler.VPORT));
    }

    /**
     * Tests whether a set internal routing call succeeds.
     * It should fail because it passes a Single Switch.
     */
    public void testSetInternalRoutingSingleSwitch() {
        //set up for creating a Single Switch
        final PhysicalSwitch sw = new PhysicalSwitch(1);
        PhysicalNetwork.getInstance().addSwitch(sw);
        super.createNetwork();
        //creates the Single Switch
        super.createSwitch(1, Collections.singletonList((long)1));

        final JSONRPC2Response resp = super.setInternalRouting(1, (long)46200400562356225L, "spf", (byte) 1);

        Assert.assertNotNull("setInternalRouting should return null", resp.getError());
    }

    /**
     * Tests whether a set internal routing call succeeds.
     * It should fail because the Big Switch is not connected
     */
    public void testSetInternalRoutingBadSwitch() {
        //set up for creating a Big Switch
        final PhysicalSwitch sw1 = new PhysicalSwitch(1);
        final PhysicalSwitch sw2 = new PhysicalSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        super.createNetwork();
        final List<Long> l = new ArrayList<Long>();
        l.add((long)1);
        l.add((long)2);
        //creates the Big Switch
        super.createSwitch(1,l);

        final JSONRPC2Response resp = super.setInternalRouting(1, (long)46200400562356225L, "spf", (byte) 1);

        Assert.assertNotNull("setInternalRouting should return null",
                resp.getError());
    }

    /**
     * Tests whether a set internal routing call succeeds.
     * It should succeed because it passes a Big Switch.
     */
    @SuppressWarnings("unchecked")
    public void testSetInternalRoutingBigSwitch() {
        //set up for creating a Big Switch
        final PhysicalSwitch sw1 = new PhysicalSwitch(1);
        final PhysicalSwitch sw2 = new PhysicalSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        PhysicalNetwork.getInstance().createLink(p1, p2);
        PhysicalNetwork.getInstance().createLink(p2, p1);
        super.createNetwork();
        final List<Long> l = new ArrayList<Long>();
        l.add((long)1);
        l.add((long)2);
        //creates the Big Switch
        super.createSwitch(1,l);

        final JSONRPC2Response resp = super.setInternalRouting(1, (long)46200400562356225L, "spf", (byte) 1);

        Assert.assertNull("setInternalRouting should not return null",
                resp.getError());

        Assert.assertTrue("setInternalRouting has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertTrue(l.equals(result.get(TenantHandler.DPIDS)));
    }

    /**
     * Tests whether a connect host call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectHost() {
        final TestSwitch sw1 = new TestSwitch(1);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        final PhysicalPort port = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        port.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        port.setPortNumber((short) 1);
        sw1.addPort(port);

        super.createNetwork();
        super.createSwitch(1, Collections.singletonList((long)1));
        super.createPort(1, (long) 1, (short) 1);
        JSONRPC2Response resp = super.connectHost(1, (long) 46200400562356225L,
                (short) 1, "00:00:00:00:00:01");

        Assert.assertNull(
                resp.getError() == null ? "ConnectHost should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("ConnectHost has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.HOST));

        // Try to create another host with same MAC
        resp = super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        Assert.assertNotNull("ConnectHost should not allow duplicate MACs",
                resp.getError());
    }

    /**
     * Tests whether a connect link call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectLink() {
        super.createNetwork();
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);

        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p2.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p2.setPortNumber((short) 1);
        sw2.addPort(p2);
        PhysicalNetwork.getInstance().createLink(p1, p2);
        PhysicalNetwork.getInstance().createLink(p2, p1);

        super.createSwitch(1, Collections.singletonList((long)1));
        super.createSwitch(1, Collections.singletonList((long)2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);

        JSONRPC2Response resp = super.connectLink(1, (long) 46200400562356225L,
                (short) 1, (long) 46200400562356226L, (short) 1, "manual",
                (byte) 0);

        Assert.assertNull(
                resp.getError() == null ? "ConnectLink should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("ConnectLink has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.LINK));

        // TODO: should this not be its own separate test?

        resp = super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);

        Assert.assertNull(
                resp.getError() == null ? "SetLinkPath should not return null"
                        : resp.getError().getMessage(), resp.getError());

        Assert.assertTrue("SetLinkPath has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        result = (Map<String, Object>) resp.getResult();

        // TODO: we should check if the path really has been set
        // can't do this now as map is not working properly in the tests

        Assert.assertEquals(1, result.get(TenantHandler.LINK));
    }

    /**
     * Tests whether a create switch route call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testConnectRoutePass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        final List<Long> l = new LinkedList<>();
        l.add((long)1);
        l.add((long)2);
        super.createSwitch(1, l);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:02");

        final JSONRPC2Response resp = super.connectRoute(1, 46200400562356225L,
                (short) 1, (short) 2, "1/1-2/1", (byte) 100);

        Assert.assertNull("ConnectOVXRoute should not return null",
                resp.getError());

        Assert.assertTrue("ConnectOVXRoute has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.ROUTE));
    }

    /**
     * Tests whether a remove network call succeeds.
     */
    public void testRemoveNetPass() {
        this.setUpTopologyOneHostTwoSwitches();
        final JSONRPC2Response resp = super.removeNetwork(1);

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a remove switch call succeeds.
     */
    public void testRemoveSwitchPass() {
        this.setUpTopologyOneHostTwoSwitches();
        final JSONRPC2Response resp = super.removeSwitch(1, 46200400562356225L);

        Assert.assertNull(resp.getError());
    }

    /**
     * Tests whether a remove port call succeeds.
     */
    public void testRemovePortPass() {
        this.setUpTopologyOneHostTwoSwitches();
        JSONRPC2Response resp = super.removePort(1, 46200400562356225L,
                (short) 1);

        Assert.assertNull(resp.getError());

        resp = super.removePort(1, 46200400562356225L,
                (short) 1);

        Assert.assertNotNull(resp.getError());
    }

    /**
     * Tests whether a remove link call succeeds.
     */
    public void testDisconnectLinkPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();

        final JSONRPC2Response resp = super.disconnectLink(1, 1);

        Assert.assertNull(resp.getError());
    }

    /**
     * Tests whether a disconnect host call succeeds.
     */
    public void testDisconnectHostPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();

        final JSONRPC2Response resp = super.disconnectHost(1, 1);

        Assert.assertNull("Disconnect Host should have not returned null",
                resp.getError());

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a disconnect route call succeeds.
     */
    public void testDisconnectRoutePass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        final List<Long> l = new LinkedList<>();
        l.add((long)1);
        l.add((long)2);
        super.createSwitch(1, l);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 1,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:02");
        super.connectRoute(1, 46200400562356225L, (short) 1, (short) 2,
                "1/1-2/1", (byte) 100);

        final JSONRPC2Response resp = super.disconnectRoute(1,
                (long) 46200400562356225L, 1);

        Assert.assertNull("Remove switch route should not return null",
                resp.getError());

        Assert.assertNull(resp.getResult());
    }

    /**
     * Tests whether a start network call succeeds
     */
    public void testStartNetPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();

        final JSONRPC2Response resp = super.startNetwork(1);

        Assert.assertNull("Start network should not return null",
                resp.getError());

        Assert.assertTrue("Start network has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));

        Assert.assertEquals(true, result.get(TenantHandler.IS_BOOTED));
    }

    /**
     * Tests whether a start switch call succeeds
     * 
     * The ports should be inactive because there are no links
     */
    public void testStartSwitchNoLinksPass() {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList((long)1));
        super.createSwitch(1, Collections.singletonList((long)2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);

        final JSONRPC2Response resp1 = super.startSwitch(1, (long) 46200400562356225L);

        Assert.assertNull("Start switch should not return null",
                resp1.getError());
        Assert.assertTrue("Start switch has incorrect return type",
                resp1.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result1 = (Map<String, Object>) resp1.getResult();

        Assert.assertEquals(1, result1.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356225L, result1.get(TenantHandler.VDPID));
        Assert.assertEquals(Collections.singletonList((long) 1), result1.get(TenantHandler.DPIDS));

        final JSONRPC2Response resp2 = super.startSwitch(1, (long) 46200400562356226L);
        Assert.assertNull("Start switch should not return null",
                resp2.getError());
        Assert.assertTrue("Start switch has incorrect return type",
                resp2.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result2 = (Map<String, Object>) resp2.getResult();

        Assert.assertEquals(1, result2.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356226L, result2.get(TenantHandler.VDPID));
        Assert.assertEquals(Collections.singletonList((long) 2), result2.get(TenantHandler.DPIDS));

        final OVXMap map = OVXMap.getInstance();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = map.getVirtualNetwork(1);
            Assert.assertTrue(virtualNetwork.getSwitch((long) 46200400562356225L).isActive());
            OVXPort port1_1 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)1);
            Assert.assertFalse(port1_1.isActive());
            OVXPort port1_2 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)2);
            Assert.assertFalse(port1_2.isActive());
            OVXPort port2_1 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)1);
            Assert.assertFalse(port2_1.isActive());
            OVXPort port2_2 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)2);
            Assert.assertFalse(port2_2.isActive());
        }
        catch (NetworkMappingException e) {
            //fails the test if the virtual network has the wrong tenantId
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        }
    }

    /**
     * Tests whether a start switch call succeeds
     * 
     * The ports should be active
     */
    public void testStartSwitchPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();
        super.startSwitch(1, (long) 46200400562356225L);

        final OVXMap map = OVXMap.getInstance();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = map.getVirtualNetwork(1);
            Assert.assertTrue(virtualNetwork.getSwitch((long) 46200400562356225L).isActive());
            OVXPort port1_1 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)1);
            Assert.assertTrue(port1_1.isActive());

            OVXPort port1_2 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)2);
            Assert.assertTrue(port1_2.isActive());

            OVXPort port2_1 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)1);
            Assert.assertTrue(port2_1.isActive());

            OVXPort port2_2 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)2);
            Assert.assertTrue(port2_2.isActive());
        } catch (NetworkMappingException e) {
            //fails the test if the virtual network has the wrong tenantId
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        }
    }

    /**
     * Tests whether a start port call succeeds
     */
    public void testStartPortPass() {
        this.setUpTopologyOneHostTwoSwitches();

        final JSONRPC2Response resp = super.startPort(1, (long) 46200400562356225L, (short) 1);

        Assert.assertNull("Start port should not return null",
                resp.getError());
        Assert.assertTrue("Start port has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356225L, result.get(TenantHandler.VDPID));
        Assert.assertEquals((long) 1, result.get(TenantHandler.DPID));
        Assert.assertEquals((short) 1, result.get(TenantHandler.PORT));
        Assert.assertEquals((short) 1, result.get(TenantHandler.VPORT));

        final OVXMap map = OVXMap.getInstance();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = map.getVirtualNetwork(1);
            OVXPort port1_1 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)1);
            Assert.assertTrue(port1_1.isActive());
            OVXPort port2_1 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)1);
            Assert.assertTrue(port2_1.isActive());
        } catch (NetworkMappingException e) {
            //fails the test if the virtual network has the wrong tenantId
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        }
    }

    /**
     * Tests whether a stop network call succeeds.
     */
    @SuppressWarnings("unchecked")
    public void testStopNetPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();
        final JSONRPC2Response resp = super.stopNetwork(1);

        Assert.assertNull("Stop network should not return null",
                resp.getError());

        Assert.assertTrue("Stop network has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        Map<String, Object> result = (Map<String, Object>) resp.getResult();

        Assert.assertEquals(1, result.get(TenantHandler.TENANT));

        Assert.assertEquals(false, result.get(TenantHandler.IS_BOOTED));
    }

    /**
     * Tests whether a stop switch call succeeds
     */
    public void testStopSwitchPass() {
        this.setUpTopologyOneHostTwoSwitchesFourPorts();

        super.startNetwork(1);
        super.startSwitch(1, (long) 46200400562356225L);
        super.startSwitch(1, (long) 46200400562356226L);

        final JSONRPC2Response resp1 = super.stopSwitch(1, (long) 46200400562356225L);

        Assert.assertNull("Stop switch should not return null",
                resp1.getError());
        Assert.assertTrue("Stop switch has incorrect return type",
                resp1.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result1 = (Map<String, Object>) resp1.getResult();

        Assert.assertEquals(1, result1.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356225L, result1.get(TenantHandler.VDPID));
        Assert.assertEquals(Collections.singletonList((long) 1), result1.get(TenantHandler.DPIDS));

        final JSONRPC2Response resp2 = super.stopSwitch(1, (long) 46200400562356226L);
        Assert.assertNull("Stop switch should not return null",
                resp2.getError());
        Assert.assertTrue("Stop switch has incorrect return type",
                resp2.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result2 = (Map<String, Object>) resp2.getResult();

        Assert.assertEquals(1, result2.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356226L, result2.get(TenantHandler.VDPID));
        Assert.assertEquals(Collections.singletonList((long) 2), result2.get(TenantHandler.DPIDS));

        final OVXMap map = OVXMap.getInstance();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = map.getVirtualNetwork(1);
            Assert.assertFalse(virtualNetwork.getSwitch((long) 46200400562356225L).isActive());
            OVXPort port1_1 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)1);
            Assert.assertFalse(port1_1.isActive());
            OVXPort port1_2 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)2);
            //connected to a host. Supposed to remain active when the switch stops
            Assert.assertTrue(port1_2.isActive());
            OVXPort port2_1 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)1);
            Assert.assertFalse(port2_1.isActive());
            OVXPort port2_2 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)2);
            //connected to a host.
            Assert.assertTrue(port2_2.isActive());
        }
        catch (NetworkMappingException e) {
            //fails the test if the virtual network has the wrong tenantId
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        }
    }

    /**
     * Tests whether a stop port call succeeds
     */
    public void testStopPortPass() {
        this.setUpTopologyOneHostTwoSwitches();

        super.startSwitch(1, (long) 46200400562356225L);

        final JSONRPC2Response resp1 = super.stopPort(1, (long) 46200400562356225L, (short) 1);

        Assert.assertNull("Stop port should not return null",
                resp1.getError());
        Assert.assertTrue("Stop port has incorrect return type",
                resp1.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result1 = (Map<String, Object>) resp1.getResult();

        Assert.assertEquals(1, result1.get(TenantHandler.TENANT));
        Assert.assertEquals(46200400562356225L, result1.get(TenantHandler.VDPID));
        Assert.assertEquals((long) 1, result1.get(TenantHandler.DPID));
        Assert.assertEquals((short) 1, result1.get(TenantHandler.PORT));
        Assert.assertEquals((short) 1, result1.get(TenantHandler.VPORT));

        final OVXMap map = OVXMap.getInstance();
        OVXNetwork virtualNetwork;
        try {
            virtualNetwork = map.getVirtualNetwork(1);
            Assert.assertTrue(virtualNetwork.getSwitch((long) 46200400562356225L).isActive());
            OVXPort port1_1 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)1);
            Assert.assertFalse(port1_1.isActive());
            OVXPort port1_2 = virtualNetwork.getSwitch((long) 46200400562356225L).getPort((short)2);
            Assert.assertTrue(port1_2.isActive());
            OVXPort port2_1 = virtualNetwork.getSwitch((long) 46200400562356226L).getPort((short)1);
            Assert.assertFalse(port2_1.isActive());
        } catch (NetworkMappingException e) {
            //fails the test if the virtual network has the wrong tenantId
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        }
    }

    public void setUpTopologyOneHostTwoSwitchesFourPorts()
    {
        // set the physical network (linear 2 sws with 1 host x sw)
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw2,
                true);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 2);
        sw2.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList((long)1));
        super.createSwitch(1, Collections.singletonList((long)2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 2, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 2);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
    }

    /**
     * set the physical network (linear 2 sws with 1 host x sw)
     */
    public void setUpTopologyOneHostTwoSwitches()
    {
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw1,
                true);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 2);
        sw1.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 1);
        sw2.addPort(p3);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);

        // set the virtual network (copy of the phy network)
        super.createNetwork();
        super.createSwitch(1, Collections.singletonList((long)1));
        super.createSwitch(1, Collections.singletonList((long)2));
        super.createPort(1, (long) 1, (short) 1);
        super.createPort(1, (long) 1, (short) 2);
        super.createPort(1, (long) 2, (short) 1);
        super.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        super.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        super.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
    }

    @Override
    protected void tearDown() throws Exception {
        OVXMap.reset();
        PhysicalNetwork.reset();
        OVXNetwork.reset();
    }
}
