package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.HexString;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import junit.framework.Assert;
import junit.framework.TestSuite;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.api.service.handlers.tenant.AbstractAPICalls;
import net.onrc.openvirtex.api.service.handlers.tenant.TestSwitch;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

public class PassingMonitoringAPI extends MonitoringAPICalls {

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(net.onrc.openvirtex.api.service.handlers.tenant.PassingAPITest.class);
        suite.addTestSuite(PassingMonitoringAPI.class);
        return suite;
    }
    
    @SuppressWarnings("unchecked")
    public void testGetPhysicalHosts()
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

        AbstractAPICalls setup = new AbstractAPICalls();
        
        // set the virtual network (copy of the phy network)
        setup.createNetwork();
        setup.createSwitch(1, Collections.singletonList((long)1));
        setup.createSwitch(1, Collections.singletonList((long)2));
        setup.createPort(1, (long) 1, (short) 1);
        setup.createPort(1, (long) 2, (short) 1);
        setup.createPort(1, (long) 1, (short) 2);
        setup.createPort(1, (long) 2, (short) 2);
        setup.connectHost(1, (long) 46200400562356225L, (short) 2,
                "00:00:00:00:00:01");
        setup.connectHost(1, (long) 46200400562356226L, (short) 2,
                "00:00:00:00:00:02");
        setup.connectLink(1, (long) 46200400562356225L, (short) 1,
                (long) 46200400562356226L, (short) 1, "manual", (byte) 0);
        setup.setLinkPath(1, 1, "1/1-2/1", (byte) 100);
        
        final JSONRPC2Response resp = super.getPhysicalHosts();
        
        Assert.assertNull(resp.getError());
        
        Assert.assertTrue("GetPhysicalHosts has incorrect return type",
                resp.getResult() instanceof List<?>);
        
        List<Object> result = (List<Object>) resp.getResult();
        
        Assert.assertTrue("GetPhysicalHosts should have 2 hosts", result.size() == 2);
        
        Assert.assertTrue("GetPhysicalHosts should return a list of HashMaps",
                result.get(0) instanceof Map<?,?> && result.get(1) instanceof Map<?,?>);
        
        HashMap<String, Object> h1;
        HashMap<String, Object> h2;
        
        if ( (int)((HashMap<String, Object>)result.get(0)).get(TenantHandler.HOST) == 1) {
            h1 = (HashMap<String, Object>) result.get(0);
            h2 = (HashMap<String, Object>) result.get(1);
        }
        else {
            h1 = (HashMap<String, Object>) result.get(1);
            h2 = (HashMap<String, Object>) result.get(0);
        }
        
        //cannot have the same hostId
        Assert.assertFalse((int)h1.get(TenantHandler.HOST) - (int)h2.get(TenantHandler.HOST) == 0);
        
        //both hosts should be connected at port 2
        Assert.assertEquals((short) 2, (short)h1.get(TenantHandler.PORT));
        Assert.assertEquals((short) 2, (short)h2.get(TenantHandler.PORT));
        
        //the list could have the maps in any order, so this checks for both orders
        Assert.assertEquals(HexString.toHexString(1L), (String)h1.get(TenantHandler.DPID));
        Assert.assertEquals(HexString.toHexString(2L), (String)h2.get(TenantHandler.DPID));
        Assert.assertEquals("00:00:00:00:00:01", (String)h1.get(TenantHandler.MAC));
        Assert.assertEquals("00:00:00:00:00:02", (String)h2.get(TenantHandler.MAC));
    }
    
    public void testGetVirtualLinkMapping()
    {
        // set the physical network
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        final TestSwitch sw3 = new TestSwitch(3);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        PhysicalNetwork.getInstance().addSwitch(sw3);
        
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 1);
        sw2.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 2);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw3,
                false);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 1);
        sw3.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);
        PhysicalNetwork.getInstance().createLink(p2, p4);
        PhysicalNetwork.getInstance().createLink(p4, p2);

        AbstractAPICalls setup = new AbstractAPICalls();
        
        // set the virtual network (copy of the phy network)
        setup.createNetwork();
        setup.createSwitch(1, Collections.singletonList((long)1));
        setup.createSwitch(1, Collections.singletonList((long)2));
        setup.createSwitch(1, Collections.singletonList((long)3));
        setup.createPort(1, (long) 1, (short) 1);
        setup.createPort(1, (long) 2, (short) 1);
        setup.createPort(1, (long) 2, (short) 2);
        setup.createPort(1, (long) 3, (short) 1);
        setup.connectLink(1, 46200400562356225L, (short) 1,
               46200400562356226L, (short) 2, "manual", (byte) 0);
        setup.connectLink(1, 46200400562356226L, (short) 1,
               46200400562356227L, (short) 1, "manual", (byte) 0);
        setup.setLinkPath(1, 1, "1/1-2/2", (byte) 100);
        setup.setLinkPath(1, 2, "2/1-3/1", (byte) 100);
        
        final JSONRPC2Response resp = super.getVirtualLinkMapping(1);
        
        Assert.assertNull(resp.getError());
        
        Assert.assertTrue("GetVirtualLinkMapping has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        
        try {
            LinkedList<LinkedList<Integer>> list1 = new LinkedList<LinkedList<Integer>>();
            LinkedList<OVXLink> vlink1 = (LinkedList<OVXLink>) OVXMap.getInstance().getVirtualNetwork(1).getLinksById(1);
            for (OVXLink link : vlink1) {
                LinkedList<Integer> path1 = new LinkedList<Integer>();
                for (PhysicalLink plink1: OVXMap.getInstance().getPhysicalLinks(link)) {
                    path1.add(plink1.getLinkId());
                }
                list1.add(path1);
            }
            Assert.assertEquals(list1, result.get(1));
        } catch (NetworkMappingException e) {
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        } catch (LinkMappingException e) {
            Assert.assertNull("Virtual link not found for link ID", e.getMessage());
        }
        
        try {
            LinkedList<LinkedList<Integer>> list2 = new LinkedList<LinkedList<Integer>>();
            LinkedList<OVXLink> vlink2 = (LinkedList<OVXLink>) OVXMap.getInstance().getVirtualNetwork(1).getLinksById(2);
            for (OVXLink link : vlink2) {
                LinkedList<Integer> path2 = new LinkedList<Integer>();
                for (PhysicalLink plink2: OVXMap.getInstance().getPhysicalLinks(link)) {
                    path2.add(plink2.getLinkId());
                }
                list2.add(path2);
            }
            Assert.assertEquals(list2, result.get(2));
        } catch (NetworkMappingException e) {
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        } catch (LinkMappingException e) {
            Assert.assertNull("Virtual link not found for link ID", e.getMessage());
        }
    }
    
    public void testGetVirtualLinkMappingSpecificLink()
    {
        // set the physical network
        final TestSwitch sw1 = new TestSwitch(1);
        final TestSwitch sw2 = new TestSwitch(2);
        final TestSwitch sw3 = new TestSwitch(3);
        PhysicalNetwork.getInstance().addSwitch(sw1);
        PhysicalNetwork.getInstance().addSwitch(sw2);
        PhysicalNetwork.getInstance().addSwitch(sw3);
        
        final PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1,
                false);
        p1.setHardwareAddress(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        p1.setPortNumber((short) 1);
        sw1.addPort(p1);
        final PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p2.setHardwareAddress(new byte[] {0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});
        p2.setPortNumber((short) 1);
        sw2.addPort(p2);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw2,
                false);
        p3.setHardwareAddress(new byte[] {0x11, 0x12, 0x13, 0x14, 0x15, 0x16});
        p3.setPortNumber((short) 2);
        sw2.addPort(p3);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw3,
                false);
        p4.setHardwareAddress(new byte[] {0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c});
        p4.setPortNumber((short) 1);
        sw3.addPort(p4);
        PhysicalNetwork.getInstance().createLink(p1, p3);
        PhysicalNetwork.getInstance().createLink(p3, p1);
        PhysicalNetwork.getInstance().createLink(p2, p4);
        PhysicalNetwork.getInstance().createLink(p4, p2);

        AbstractAPICalls setup = new AbstractAPICalls();
        
        // set the virtual network (copy of the phy network)
        setup.createNetwork();
        setup.createSwitch(1, Collections.singletonList((long)1));
        setup.createSwitch(1, Collections.singletonList((long)2));
        setup.createSwitch(1, Collections.singletonList((long)3));
        setup.createPort(1, (long) 1, (short) 1);
        setup.createPort(1, (long) 2, (short) 1);
        setup.createPort(1, (long) 2, (short) 2);
        setup.createPort(1, (long) 3, (short) 1);
        setup.connectLink(1, 46200400562356225L, (short) 1,
               46200400562356226L, (short) 2, "manual", (byte) 0);
        setup.connectLink(1, 46200400562356226L, (short) 1,
               46200400562356227L, (short) 1, "manual", (byte) 0);
        setup.setLinkPath(1, 1, "1/1-2/2", (byte) 100);
        setup.setLinkPath(1, 2, "2/1-3/1", (byte) 100);
        
        final JSONRPC2Response resp = super.getVirtualLinkMapping(1,1);
        
        Assert.assertNull(resp.getError());
        
        Assert.assertTrue("GetVirtualLinkMapping has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        
        try {
            LinkedList<LinkedList<Integer>> list1 = new LinkedList<LinkedList<Integer>>();
            LinkedList<OVXLink> vlink1 = (LinkedList<OVXLink>) OVXMap.getInstance().getVirtualNetwork(1).getLinksById(1);
            for (OVXLink link : vlink1) {
                LinkedList<Integer> path1 = new LinkedList<Integer>();
                for (PhysicalLink plink1: OVXMap.getInstance().getPhysicalLinks(link)) {
                    path1.add(plink1.getLinkId());
                }
                list1.add(path1);
            }
            Assert.assertEquals(list1, result.get(1));
        } catch (NetworkMappingException e) {
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        } catch (LinkMappingException e) {
            Assert.assertNull("Virtual link not found for link ID", e.getMessage());
        }
        Assert.assertNull(result.get(2));
    }
    
    public void testGetVirtualSwitchMapping()
    {
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

        final PhysicalSwitch sw3 = new PhysicalSwitch(3);
        final PhysicalSwitch sw4 = new PhysicalSwitch(4);
        PhysicalNetwork.getInstance().addSwitch(sw3);
        PhysicalNetwork.getInstance().addSwitch(sw4);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw3,
                false);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw4,
                false);
        PhysicalNetwork.getInstance().createLink(p3, p4);
        PhysicalNetwork.getInstance().createLink(p4, p3);
        
        // set the virtual network (copy of the phy network)
        AbstractAPICalls setup = new AbstractAPICalls();
        
        setup.createNetwork();
        final List<Long> l1 = new LinkedList<>();
        l1.add((long)1);
        l1.add((long)2);
        setup.createSwitch(1, l1);
        
        final List<Long> l2 = new LinkedList<>();
        l2.add((long)3);
        l2.add((long)4);
        setup.createSwitch(1, l2);
        
        final JSONRPC2Response resp = super.getVirtualSwitchMapping(1);

        Assert.assertNull(resp.getError());

        Assert.assertTrue("GetVirtualLinkMapping has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        
        Assert.assertTrue(result.get(HexString.toHexString(46200400562356225L)) instanceof Map<?, ?>);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> bigSwitch1 = (Map<String, Object>) result.get(HexString.toHexString(46200400562356225L));
        @SuppressWarnings("unchecked")
        Map<String, Object> bigSwitch2 = (Map<String, Object>) result.get(HexString.toHexString(46200400562356226L));
        try {
            OVXSwitch vSwitch1 = OVXMap.getInstance().getVirtualNetwork(1).getSwitch(46200400562356225L);
            LinkedList<String> pSwitchNames = new LinkedList<String>();
            for (PhysicalSwitch pSwitch : OVXMap.getInstance().getPhysicalSwitches(vSwitch1)) {
                pSwitchNames.add(pSwitch.getSwitchName());
            }
            Assert.assertEquals(pSwitchNames, bigSwitch1.get(MonitoringHandler.SWITCHES));
            pSwitchNames.clear();
            
            OVXSwitch vSwitch2 = OVXMap.getInstance().getVirtualNetwork(1).getSwitch(46200400562356226L);
            for (PhysicalSwitch pSwitch : OVXMap.getInstance().getPhysicalSwitches(vSwitch2)) {
                pSwitchNames.add(pSwitch.getSwitchName());
            }
            Assert.assertEquals(pSwitchNames, bigSwitch2.get(MonitoringHandler.SWITCHES));
        } catch (InvalidDPIDException e) {
            Assert.assertNull("Physical Switch not found for dpid", e.getMessage());
        } catch (NetworkMappingException e) {
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        } catch (SwitchMappingException e) {
            Assert.assertNull("Switch not found on this map", e.getMessage());
        }
    }
    
    public void testGetVirtualSwitchMappingSpecificSwitch()
    {
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

        final PhysicalSwitch sw3 = new PhysicalSwitch(3);
        final PhysicalSwitch sw4 = new PhysicalSwitch(4);
        PhysicalNetwork.getInstance().addSwitch(sw3);
        PhysicalNetwork.getInstance().addSwitch(sw4);
        final PhysicalPort p3 = new PhysicalPort(new OFPhysicalPort(), sw3,
                false);
        final PhysicalPort p4 = new PhysicalPort(new OFPhysicalPort(), sw4,
                false);
        PhysicalNetwork.getInstance().createLink(p3, p4);
        PhysicalNetwork.getInstance().createLink(p4, p3);
        
        // set the virtual network (copy of the phy network)
        AbstractAPICalls setup = new AbstractAPICalls();
        
        setup.createNetwork();
        final List<Long> l1 = new LinkedList<>();
        l1.add((long)1);
        l1.add((long)2);
        setup.createSwitch(1, l1);
        
        final List<Long> l2 = new LinkedList<>();
        l2.add((long)3);
        l2.add((long)4);
        setup.createSwitch(1, l2);
        
        final JSONRPC2Response resp = super.getVirtualSwitchMapping(1, 46200400562356225L);

        Assert.assertNull(resp.getError());

        Assert.assertTrue("GetVirtualLinkMapping has incorrect return type",
                resp.getResult() instanceof Map<?, ?>);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        
        Assert.assertTrue(result.get(HexString.toHexString(46200400562356225L)) instanceof Map<?, ?>);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> bigSwitch1 = (Map<String, Object>) result.get(HexString.toHexString(46200400562356225L));
        try {
            OVXSwitch vSwitch1 = OVXMap.getInstance().getVirtualNetwork(1).getSwitch(46200400562356225L);
            //System.out.print(vSwitch1.getSwitchId());
            LinkedList<String> pSwitchNames = new LinkedList<String>();
            for (PhysicalSwitch pSwitch : OVXMap.getInstance().getPhysicalSwitches(vSwitch1)) {
                pSwitchNames.add(pSwitch.getSwitchName());
            }
            Assert.assertEquals(pSwitchNames, bigSwitch1.get(MonitoringHandler.SWITCHES));
        } catch (InvalidDPIDException e) {
            Assert.assertNull("Physical Switch not found for dpid", e.getMessage());
        } catch (NetworkMappingException e) {
            Assert.assertNull("Virtual network not found for tenant with ID", e.getMessage());
        } catch (SwitchMappingException e) {
            Assert.assertNull("Switch not found on this map", e.getMessage());
        }
        Assert.assertNull(result.get(HexString.toHexString(46200400562356226L)));
    }
    
    @Override
    protected void tearDown() throws Exception {
        OVXMap.reset();
        PhysicalNetwork.reset();
        OVXNetwork.reset();
    }
}
