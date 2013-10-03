/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.messages.OVXFlowMod;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;

public class FlowTableTest extends TestCase {

    public FlowTableTest(final String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(FlowTableTest.class);
    }

    public void testAddFlowMod() {
	final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	final OVXFlowTable oft = new OVXFlowTable(vsw);
	final OVXFlowMod fm1 = new OVXFlowMod();

	final long c1 = (long) vsw.getTenantId() << 32 | 1;
	final long c = oft.addFlowMod(fm1);

	Assert.assertEquals(c, c1);
    }

    public void testDeleteFlowMod() {
	final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	final OVXFlowTable oft = new OVXFlowTable(vsw);
	final OVXFlowMod fm1 = new OVXFlowMod();

	final long c = oft.addFlowMod(fm1);
	final OVXFlowMod fm2 = oft.deleteFlowMod(c);

	Assert.assertEquals(fm1, fm2);
    }

    public void testGenerateCookie() {
	final OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	final OVXFlowTable oft = new OVXFlowTable(vsw);

	final OVXFlowMod fm1 = new OVXFlowMod();
	final OVXFlowMod fm2 = new OVXFlowMod();
	final OVXFlowMod fm3 = new OVXFlowMod();

	final long c1 = (long) vsw.getTenantId() << 32 | 1;
	final long c2 = (long) vsw.getTenantId() << 32 | 2;

	// generate new cookies while none in freelist
	long c = oft.addFlowMod(fm1);
	Assert.assertEquals(c, c1);
	c = oft.addFlowMod(fm2);
	Assert.assertEquals(c, c2);

	// should re-use first cookie that was freed up
	oft.deleteFlowMod(c1);
	c = oft.addFlowMod(fm3);
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
	final OVXFlowMod base_fm = new OVXFlowMod();
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
	final OVXFlowMod fm = new OVXFlowMod();
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

	/* try add with overlap check off -xs should succeed. */
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
    }

    @Override
    protected void tearDown() throws Exception {
	super.tearDown();
    }
}
