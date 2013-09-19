package net.onrc.openvirtex.elements.datapath;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.messages.OVXFlowMod;

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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
