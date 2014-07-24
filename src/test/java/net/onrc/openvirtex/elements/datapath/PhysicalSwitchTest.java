/*
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
 * ****************************************************************************
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsReply;
import net.onrc.openvirtex.routing.RoutingAlgorithms;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Tests for PhysicalSwitchTests.
 */
public class PhysicalSwitchTest extends TestCase {
    private OpenVirteXController ctl = null;
    private Mappable             map = null;

    public PhysicalSwitchTest(final String name) {
        super(name);
    }

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(PhysicalSwitchTest.class);
    }

    /*
     * SwitchDeregAction() removes the OVXSwitch from the PhysicalSwitch
     * Testing if OVXSwitches getting OFF correctly
     */
    public void testSwitchDeregAction() {
        final int tid = 10;
        final long vswId = 5;
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        final ArrayList<String> ctlList = new ArrayList<String>();
        ctlList.add("tcp:127.0.0.1:6633");
        final OVXIPAddress ip = new OVXIPAddress("10.0.0.0", tid);
        psw.register();
        final OVXSwitch vsw = new OVXSingleSwitch(vswId, tid);

        final OVXNetwork net = new OVXNetwork(tid, ctlList, ip, (short) 24);
        net.register();
        this.map.addSwitches(Collections.singletonList(psw), vsw);
        vsw.map.addNetwork(net);
        vsw.register();

        // Test: before unregister
        Assert.assertTrue(psw.map.hasVirtualSwitch(psw, tid));
        psw.cleanUpTenant(tid, (short) 0);
        vsw.tearDown(true);
        vsw.unregSwitch(true);
        // Test: after unregister
        Assert.assertFalse(psw.map.hasVirtualSwitch(psw, tid));
    }

    /*
     * getOVXPortNumber() returns the OVXPortNumber given PhysicalPortNumber,
     * TenantID and VirtualLinkId.
     * Testing if returned portnumber is correct or not
     */
    public void testgetOVXPortNumber() throws IndexOutOfBoundException,
    RoutingAlgorithmException, PortMappingException {
        final int tid = 10;
        final long vswId = 5;
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x11,
                0x12, 0x13, 0x14, 0x15, 0x01 }, (short) 1);
        psw.boot();
        pp.register();
        final PhysicalSwitch psw2 = new DummyPhysicalSwitch(2);
        psw2.register();
        final PhysicalPort pp2 = new TestPort(psw2, false, new byte[] { 0x11,
                0x12, 0x13, 0x14, 0x15, 0x02 }, (short) 2);
        psw2.boot();
        pp2.register();
        pp.boot();
        pp2.boot();
        final PhysicalLink plink = new PhysicalLink(pp, pp2);
        final PhysicalLink plink2 = new PhysicalLink(pp2, pp);
        plink.boot();
        plink2.boot();
        final ArrayList<String> ctlList = new ArrayList<String>();
        ctlList.add("tcp:127.0.0.1:6633");
        final OVXIPAddress ip = new OVXIPAddress("10.0.0.0", tid);
        final OVXSwitch vsw = new OVXSingleSwitch(vswId, tid);
        vsw.register();
        this.map.addSwitches(Collections.singletonList(psw), vsw);
        final OVXPort vp = new OVXPort(tid, pp, false, (short) 10);
        vsw.boot();
        vp.register();
        final OVXSwitch vsw2 = new OVXSingleSwitch(vswId + 1, tid);
        vsw2.register();
        this.map.addSwitches(Collections.singletonList(psw2), vsw2);
        final OVXPort vp2 = new OVXPort(tid, pp2, false, (short) 11);
        vsw2.boot();
        vp2.register();
        PhysicalNetwork.getInstance().createLink(pp, pp2);
        PhysicalNetwork.getInstance().createLink(pp2, pp);
        final RoutingAlgorithms ralg = new RoutingAlgorithms("spf", (byte) 0);
        final OVXLink vlink = new OVXLink(plink.getLinkId(), tid, vp, vp2, ralg);
        final OVXLink vlink2 = new OVXLink(plink2.getLinkId(), tid, vp2, vp,
                ralg);
        vlink.register();
        vlink2.register();
        vp.boot();
        pp.setOVXPort(vp);
        vp2.boot();
        pp2.setOVXPort(vp2);

        final OVXNetwork net = new OVXNetwork(tid, ctlList, ip, (short) 24);
        net.register();
        vsw.map.addNetwork(net);
        vsw2.map.addNetwork(net);

            // Test fails
            Assert.fail("Exception: IndexOutOfBound");

        Assert.assertEquals(
                psw.portMap.get(pp.getPortNumber())
                .getOVXPort(tid, plink.getLinkId()).getPortNumber(),
                (short) 10);
        Assert.assertEquals(
                psw2.portMap.get(pp2.getPortNumber())
                .getOVXPort(tid, plink.getLinkId()).getPortNumber(),
                (short) 11);
    }

    /*
     * addIface() adds the PhysicalPorts to the PhysicalSwitch Only
     * if the PhysicalSwitch is Active.
     * Testing if ports are getting added in the Active State Only and
     * correctly.
     */
    public void testaddIface() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x11,
                0x12, 0x13, 0x14, 0x15, 0x01 }, (short) 1);
        pp.register();
        // test: When both switch and port are not active
        Assert.assertFalse(psw.addPort(pp));
        // Booting to get into active state
        psw.boot();
        pp.boot();
        // Test: When both switch and port are active
        Assert.assertTrue(psw.addPort(pp));
    }

    /*
     * Testing if Physicalswitch teardown() fuction is correctly functioning by
     * Checking its returned value
     * Checking if the connected channel is connected anymore(after the
     * teardown() call)
     */
    public void testteardown() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        pp.register();
        psw.boot();
        pp.boot();
        // Before teardown psw.channel.isConnected() should be true
        Assert.assertTrue(psw.channel.isConnected());
        // Test: teardown should return true
        Assert.assertTrue(psw.tearDown());
        // After teardown: Channel shouldn't be connected
        Assert.assertFalse(psw.channel.isConnected());
    }

    // Testing if port is removed correctly
    public void testremovePort() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        psw.boot();
        pp.register();
        // before psw.remove(): port should be in portmap
        Assert.assertNotNull(psw.portMap.get(pp.getPortNumber()));
        psw.removePort(pp);
        // after psw.remove(): port should not be in portmap
        Assert.assertNull(psw.portMap.get(pp.getPortNumber()));
    }

    /*
     * Boot() adds the PhysicalSwitch to PhysicalNetwork and
     * changes it state to Active
     * Testing the above mentioned functionality.
     */
    public void testboot() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        // Switch (psw) must be inActive before boot
        Assert.assertFalse(psw.isActive());
        /*
         * Test: if boot() call fillportmap() and if fillportmap()
         * fill the port to the portmap correctly
         */
        // portmap before the boot() call. (Should be empty)
        Assert.assertTrue(psw.portMap.isEmpty());
        psw.boot();
        // psw should Active after boot
        Assert.assertTrue(psw.isActive());
        // Test: after boot() call port 'pp' should get added to psw-portmap
        Assert.assertFalse(psw.portMap.isEmpty());
    }

    // testing register is correctly adds Physical switch to Physical Network
    public void testregister() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        // before register(): Switch psw should not be in Physical Network
        Assert.assertNull(PhysicalNetwork.getInstance().getSwitch((long) 1));
        psw.register();
        // after register() call: Switch should get added to Physical Network.
        Assert.assertNotNull(PhysicalNetwork.getInstance().getSwitch((long) 1));
    }

    /*
     * Testing if unregister correctly remove physical switch from physical
     * network
     */
    public void testunregister() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        // Test: Switch (psw) is in the physical network
        Assert.assertNotNull(PhysicalNetwork.getInstance().getSwitch((long) 1));
        psw.unregister();
        // Test: Switch (psw) correctly removed from the pysical network
        Assert.assertNull(PhysicalNetwork.getInstance().getSwitch((long) 1));

    }

    // Testing physical switch sendMsg method not (complete at the moment)
    public void testsendMsg() {
        final OFMessage ofmsg = new OFMessage();
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        /*
         * Test: before calling sendMsg() channel member msgSent should be false
         * (Overridden in isBound())
         */
        Assert.assertFalse(psw.channel.isBound());
        psw.sendMsg(ofmsg, psw);
        // Test: after sendMsg() call msgSent should not be null
        Assert.assertTrue(psw.channel.isBound());
    }

    /*
     * setportstatus() set the specified port to portmap of PhysicalSwitch
     * Testing the mentioned functionality
     */
    public void testsetportstats() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        psw.boot();
        pp.register();
        final OVXPortStatisticsReply ovxpsreply = new OVXPortStatisticsReply();
        ovxpsreply.setPortNumber((short) 10);
        final Map<Short, OVXPortStatisticsReply> statsmap = new HashMap<Short, OVXPortStatisticsReply>();
        statsmap.put((short) 10, ovxpsreply);
        // Test: Before setPortStatistics() call
        Assert.assertNull(psw.getPortStat((short) 10));
        psw.setPortStatistics(statsmap);
        // Test: after setPortStatistics() call
        Assert.assertNotNull(psw.getPortStat((short) 10));
    }

    /*
     * testing if flow mod is setting correctly when calling setflowstats()
     * method of physical switch
     */
    public void testsetflowstats() {
        final Integer tid = new Integer(1);
        final long cookieId = 1;
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        psw.boot();
        pp.register();
        final OVXFlowStatisticsReply ovxfsreply = new OVXFlowStatisticsReply();
        ovxfsreply.setCookie(cookieId);
        ovxfsreply.setPriority((short) 100);
        final Map<Integer, List<OVXFlowStatisticsReply>> flowmap = new HashMap<Integer, List<OVXFlowStatisticsReply>>();
        flowmap.put(tid, Collections.singletonList(ovxfsreply));
        // test: before calling setFlowStatistics() -> Should be null
        Assert.assertNull(psw.getFlowStats(tid));
        psw.setFlowStatistics(flowmap);
        // test: after calling setFlowStatistics() -> Should not be null
        Assert.assertNotNull(psw.getFlowStats(tid));
        // Test: If the cookieId set to specified cookiedId or not.
        Assert.assertEquals(psw.getFlowStats(tid).get(0).getCookie(), cookieId);
    }

    // testing if removeFlowMods() calls sendMsg() method to remove flows
    public void testremoveFlowMods() {
        final Integer tid = new Integer(1);
        // cookieId is in the 2nd 32bit portion of 64bit long
        final long cookieId = 4294967297l;
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        pp.register();
        pp.boot();
        final OVXFlowStatisticsReply ovxfsreply = new OVXFlowStatisticsReply();
        ovxfsreply.setCookie(cookieId);
        ovxfsreply.setPriority((short) 100);
        final OFMatch ofmatch = new OFMatch();
        ovxfsreply.setMatch(ofmatch);
        final Map<Integer, List<OVXFlowStatisticsReply>> flowmap = new HashMap<Integer, List<OVXFlowStatisticsReply>>();
        flowmap.put(tid, Collections.singletonList(ovxfsreply));
        psw.setFlowStatistics(flowmap);
        // test: before calling removeFlowMods() -> Should not be null
        Assert.assertNotNull(psw.getFlowStats(tid));
        final OVXStatisticsReply ovxsreply = new OVXStatisticsReply();
        ovxsreply.setXid(tid << 16);
        ovxsreply.setStatisticType(OFStatisticsType.FLOW);
        ovxsreply.setStatistics(Collections.singletonList(ovxfsreply));
        /*
         * Test: before calling removeFlowmods() channel member msgSent should
         * be false (Overridden in isBound())
         */
        Assert.assertFalse(psw.channel.isBound());
        psw.removeFlowMods(ovxsreply);
        /*
         * Test: after removeFlowmods() call the flow mod msg will be send so
         * msgSent should not be false
         */
        Assert.assertTrue(psw.channel.isBound());
    }

    /*
     * testing if translate() and untanslates methods translating and
     * untranslating Xid correctly
     */
    public void testtranslate() {
        final long vswId = 10;
        final Integer tid = 1;
        final int oldxid = 100;
        int newxid;
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        psw.boot();
        pp.register();
        pp.boot();
        final OVXSwitch vsw = new OVXSingleSwitch(vswId, tid);
        vsw.register();
        vsw.boot();
        final OFMessage ofmsg = new OFMessage();
        ofmsg.setXid(oldxid);
        newxid = psw.translate(ofmsg, vsw);
        ofmsg.setXid(newxid);
        // testing if oldxid value is same after untranslating
        Assert.assertEquals(psw.untranslate(ofmsg).getXid(), oldxid);
    }

    // testing if removeport() correctly remove port from the physical switch
    public void testremoveport() {
        final PhysicalSwitch psw = new DummyPhysicalSwitch(1);
        psw.register();
        final PhysicalPort pp = new TestPort(psw, false, new byte[] { 0x01,
                0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 10);
        psw.boot();
        pp.register();
        pp.boot();
        // Test: before removeport() method -> Must not be null
        Assert.assertNotNull(psw.getPort(pp.getPortNumber()));
        psw.removePort(pp);
        /*
         * Test: after removeport() method call -> Port must be get delete,
         * so should be null
         */
        Assert.assertNull(psw.getPort(pp.getPortNumber()));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.ctl = new OpenVirteXController(new CmdLineSettings());
        this.map = OVXMap.getInstance();
        PhysicalNetwork.getInstance().register();
        PhysicalNetwork.getInstance().boot();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        OVXMap.reset();
        PhysicalNetwork.getInstance().tearDown();
        PhysicalNetwork.getInstance().unregister();
        OVXNetwork.reset();
    }
}