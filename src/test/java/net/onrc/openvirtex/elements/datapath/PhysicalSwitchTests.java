/* Copyright 2014 Open Networking Laboratory
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
package net.onrc.openvirtex.elements.datapath;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.LinkedList;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.TestSwitch;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch.SwitchState;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.util.MACAddress;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.api.service.handlers.tenant.PassingAPITest.*;
import net.onrc.openvirtex.elements.host.*;
import net.onrc.openvirtex.elements.datapath.TestPort;

/**
 * Tests for PhysicalSwitchTests (Fahad).
 */
public class PhysicalSwitchTests extends TestCase {
	private OpenVirteXController ctl = null;
	private Mappable map = null;


	public PhysicalSwitchTests(final String name) {
		super(name);
	}
	/**
	 * @return the suite of tests being tested
	 */
	public static TestSuite suite() {
		return new TestSuite(PhysicalSwitchTests.class);
	}
	public void testSwitchDeregAction(){
		int tid = 10;
		long vswId=5;
		boolean before=false, after=true;
		PhysicalSwitch psw = new TestSwitch(1);
		final ArrayList <String>ctlList= new ArrayList <String>();
		ctlList.add("tcp:127.0.0.1:6633");
		OVXIPAddress ip = new  OVXIPAddress("10.0.0.0", tid);
		psw.register();
		OVXSwitch vsw = new OVXSingleSwitch(vswId, tid);
		try {
			OVXNetwork net = new OVXNetwork(tid,ctlList,ip,(short)24);
			net.register();
			this.map.addSwitches(Collections.singletonList(psw), vsw);
			vsw.map.addNetwork(net);
			vsw.register();
		}
		catch (IndexOutOfBoundException e) {
			//Test fails
			Assert.fail("Exception: IndexOutOfBound");
		}
		//Test: before unregister
		before=psw.map.hasVirtualSwitch(psw, tid);
		Assert.assertTrue(before);
		psw.cleanUpTenant(tid, (short)0);
		vsw.tearDown(true);
		vsw.unregSwitch(true);
		//Test: after unregister
		after=psw.map.hasVirtualSwitch(psw, tid);
		Assert.assertFalse(after);
	}
	public void testgetOVXPortNumber() throws IndexOutOfBoundException, RoutingAlgorithmException, PortMappingException{
		int tid = 10;
		long vswId=5;
		PhysicalSwitch psw = new TestSwitch(1);
		psw.register();
		PhysicalPort pp =new TestPort(psw, false,new byte[] { 0x11, 0x12, 0x13, 0x14, 0x15, 0x01 },(short)1);
		pp.register();
		PhysicalSwitch psw2 = new TestSwitch(2);
		psw2.register();
		PhysicalPort pp2 =new TestPort(psw, false,new byte[] { 0x11, 0x12, 0x13, 0x14, 0x15, 0x02 },(short)2);
		pp2.register();
		final ArrayList <String>ctlList= new ArrayList <String>();
		ctlList.add("tcp:127.0.0.1:6633");
		OVXIPAddress ip = new  OVXIPAddress("10.0.0.0", tid);
		OVXSwitch vsw = new OVXSingleSwitch(vswId, tid);
		vsw.register();
		map.addSwitches(Collections.singletonList(psw), vsw);
		OVXPort vp= new OVXPort(tid,pp,false,(short)10);
		vp.register();
		OVXSwitch vsw2 = new OVXSingleSwitch(vswId+1, tid);
		vsw2.register();
		map.addSwitches(Collections.singletonList(psw2), vsw2);
		OVXPort vp2= new OVXPort(tid,pp2,false,(short)11);
		vp2.register();
		PhysicalLink plink = new PhysicalLink(pp,pp2);
		plink.register();
		PhysicalNetwork.getInstance().addSwitch(psw);
		PhysicalNetwork.getInstance().addSwitch(psw2);
		PhysicalNetwork.getInstance().addPort(pp);
		PhysicalNetwork.getInstance().addPort(pp2);
		PhysicalNetwork.getInstance().createLink(pp, pp2);
		PhysicalNetwork.getInstance().createLink(pp2, pp);
		psw.portMap.put(pp.getPortNumber(), pp);
		psw2.portMap.put(pp2.getPortNumber(), pp2);
		RoutingAlgorithms ralg= new RoutingAlgorithms("spf", (byte)0);
		Integer linkId = new Integer(2);
		OVXLink vlink = new OVXLink(linkId, tid,vp, vp2,ralg) ;
		OVXLink vlink2 = new OVXLink(linkId+1, tid,vp2, vp,ralg) ;
		vlink.register();
		vlink2.register();
		vp.boot();
		pp.setOVXPort(vp);
		vp2.boot();
		pp2.setOVXPort(vp2);
		try {
			OVXNetwork net = new OVXNetwork(tid,ctlList,ip,(short)24);
			net.register();
			vsw.map.addNetwork(net);
			vsw2.map.addNetwork(net);
		}
		catch (IndexOutOfBoundException e) {
			//Test fails
			Assert.fail("Exception: IndexOutOfBound");
		}
		Assert.assertEquals( psw.portMap.get(pp.getPortNumber()).getOVXPort(tid, linkId).getPortNumber(), (short)10);
		Assert.assertEquals( psw2.portMap.get(pp2.getPortNumber()).getOVXPort(tid, linkId+1).getPortNumber(), (short)11);
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