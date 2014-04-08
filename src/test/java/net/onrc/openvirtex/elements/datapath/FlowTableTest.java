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
package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.messages.OVXFlowMod;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

public class FlowTableTest extends TestCase {
	
	OpenVirteXController ctl = null;

	public OVXFlowMod getFlowMod() {
		OVXFlowMod fm = new OVXFlowMod();
		fm.setMatch(new OFMatch()).setActions(new ArrayList<OFAction>());
		return fm;
	}
	
    public FlowTableTest(final String name) {
    	super(name);
    }

    public static Test suite() {
    	return new TestSuite(FlowTableTest.class);
    }

    public void testAddFlowMod() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
		final OVXFlowTable oft = new OVXFlowTable(vsw);
		final OVXFlowMod fm1 = this.getFlowMod();
	
		final long c1 = (long) vsw.getTenantId() << 32 | 1;
		final boolean res1 = oft.handleFlowMods(fm1);
		final long c2 = oft.getCookie(fm1, false);
		
		/* verify inital cookie value */
		Assert.assertTrue(res1);
		Assert.assertEquals(c2, c1);
		
		/* try to add identical FlowMod, should get back old cookie 
		 * since we're displacing the old flowMod */
		final boolean res2 = oft.handleFlowMods(fm1);
		final long c3 = oft.getCookie(fm1, false);
		
		Assert.assertTrue(res2);
		Assert.assertEquals(c2, c3);
		
		/* the next available cookie value should be next-value up, 
		 * which was never used */
		final long c4 = (long) vsw.getTenantId() << 32 | 2;
		Assert.assertEquals(c4 ,oft.getCookie());
    }

    public void testDeleteFlowMod() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
		final OVXFlowTable oft = new OVXFlowTable(vsw);
		final OVXFlowMod fm1 = this.getFlowMod();
		
		oft.handleFlowMods(fm1);
		final long c = oft.getCookie(fm1, false);
		final OVXFlowMod fm2 = oft.deleteFlowMod(c);
	
		Assert.assertEquals(fm1, fm2);
    }

    public void testGenerateCookie() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
		final OVXFlowTable oft = new OVXFlowTable(vsw);
		
		/* 2 FMs that deliberately don't match */
		final OVXFlowMod fm1 = this.getFlowMod();
			fm1.setCommand(OFFlowMod.OFPFC_ADD)
			.setMatch((new OFMatch())
					.setInputPort((short)1)
					.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT));
		final OVXFlowMod fm2 = this.getFlowMod();
			fm2.setCommand(OFFlowMod.OFPFC_ADD)
			.setMatch((new OFMatch()).setInputPort((short)2)
					.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT));
		
		final long c1 = (long) vsw.getTenantId() << 32 | 1;
		final long c2 = (long) vsw.getTenantId() << 32 | 2;
		
		// generate new cookies while none in freelist
		oft.handleFlowMods(fm1);
		final long c3 = oft.getCookie(fm1, false);
		
		Assert.assertEquals(c3, c1);
		oft.handleFlowMods(fm2);
		final long c4 = oft.getCookie(fm2, false);
		
		Assert.assertEquals(c4, c2);
	
		// should re-use first cookie that was freed up 
		oft.deleteFlowMod(c1);
		long c = oft.getCookie();
		Assert.assertEquals(c, c1);
    }

    /** test various Flow Entry match types. */
    public void testFlowEntryCompare() {
		final OFMatch base_m = new OFMatch();
		base_m.setDataLayerDestination(
		        new byte[] { 0x11, 0x22, 0x33, (byte) 0xab, (byte) 0xcd,
		                (byte) 0xef })
		        .setInputPort((short) 23)
		        .setNetworkDestination(5692)
		        .setWildcards(
		                OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_DST
		                        & ~OFMatch.OFPFW_IN_PORT
		                        & ~OFMatch.OFPFW_NW_DST_ALL);
		final OVXFlowMod base_fm = this.getFlowMod();
		base_fm.setBufferId(1).setMatch(base_m).setPriority((short) 20);
		final OVXFlowEntry base_fe = new OVXFlowEntry(base_fm, 11);
	
		/* a clone should be identical so be equal */
		final OFMatch equal_m = base_m.clone();
		Assert.assertEquals(base_fe.compare(equal_m, true), OVXFlowEntry.EQUAL);
	
		/* a superset match should make base_m its subset */
		final OFMatch super_m = new OFMatch();
		super_m.setInputPort((short) 23)
		        .setNetworkDestination(5692)
		        .setWildcards(
		                OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT
		                        & ~OFMatch.OFPFW_NW_DST_ALL);
		Assert.assertEquals(base_fe.compare(super_m, true), OVXFlowEntry.SUBSET);
		/* not strict - consider subset match to also be equal */
		Assert.assertEquals(base_fe.compare(super_m, false), OVXFlowEntry.EQUAL);
	
		/* a subset match should make base_m its superset */
		final OFMatch sub_m = new OFMatch();
		sub_m.setDataLayerDestination(
		        new byte[] { 0x11, 0x22, 0x33, (byte) 0xab, (byte) 0xcd,
		                (byte) 0xef })
		        .setDataLayerSource(
		                new byte[] { 0x11, 0x22, 0x33, (byte) 0xaa,
		                        (byte) 0xcc, (byte) 0xee })
		        .setInputPort((short) 23)
		        .setNetworkDestination(5692)
		        .setWildcards(
		                OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_DST
		                        & ~OFMatch.OFPFW_DL_SRC
		                        & ~OFMatch.OFPFW_IN_PORT
		                        & ~OFMatch.OFPFW_NW_DST_ALL);
		Assert.assertEquals(base_fe.compare(sub_m, true), OVXFlowEntry.SUPERSET);
	
		/* a incomparable OFMatch should return base_m to be disjoint */
		final OFMatch disj_m = new OFMatch();
		disj_m.setInputPort((short) 20).setWildcards(
		        OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT);
		Assert.assertEquals(base_fe.compare(disj_m, true),
		        OVXFlowEntry.DISJOINT);
    }
    
    /* main FlowTable operations */
    public void testHandleFlowMod() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
		final PhysicalSwitch psw = new PhysicalSwitch(0);
		ArrayList<PhysicalSwitch> l = new ArrayList<PhysicalSwitch>();
		l.add(psw);
		OVXMap.getInstance().addSwitches(l, vsw);
		final OVXFlowTable oft = new OVXFlowTable(vsw);
		final OFMatch base_m = new OFMatch();
		base_m.setDataLayerDestination(
		        new byte[] { 0x11, 0x22, 0x33, (byte) 0xab, (byte) 0xcd,
		                (byte) 0xef })
		        .setInputPort((short) 23)
		        .setNetworkDestination(5692)
		        .setWildcards(
		                OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_DST
		                        & ~OFMatch.OFPFW_IN_PORT
		                        & ~OFMatch.OFPFW_NW_DST_ALL);
		final OVXFlowMod fm = this.getFlowMod();
		fm.setBufferId(1).setMatch(base_m).setPriority((short) 20)
		        .setCommand(OFFlowMod.OFPFC_MODIFY);
	
		/* add done via modify call - should work */
		Assert.assertTrue(oft.handleFlowMods(fm));
	
		/* try strict add with superset match - should fail */
		final OFMatch super_m = new OFMatch();
		super_m.setInputPort((short) 23)
		        .setNetworkDestination(5692)
		        .setWildcards(
		                OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT
		                        & ~OFMatch.OFPFW_NW_DST_ALL);
		fm.setCommand(OFFlowMod.OFPFC_ADD)
		        .setFlags(OFFlowMod.OFPFF_CHECK_OVERLAP).setMatch(super_m);
		Assert.assertFalse(oft.handleFlowMods(fm));
	
		/* try add with overlap check off should succeed. */
		fm.setFlags((short) 0);
		Assert.assertTrue(oft.handleFlowMods(fm));
	
		/* do a delete of one element, then a wild-card. */
		fm.setCommand(OFFlowMod.OFPFC_DELETE_STRICT);
		Assert.assertTrue(oft.handleFlowMods(fm));
		fm.setCommand(OFFlowMod.OFPFC_ADD);
		oft.handleFlowMods(fm);
		/* OFPFW_ALL match - need to do sendSouth() */
		/*
		 * fm.setMatch(new OFMatch()).setCommand(OFFlowMod.OFPFC_DELETE);
		 * assertFalse(oft.handleFlowMods(fm));
		 * oft.dump();
		 */
    }

    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    	ctl = new OpenVirteXController(new CmdLineSettings());
    }

    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    }
}
