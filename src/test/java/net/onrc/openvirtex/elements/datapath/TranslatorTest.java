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
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;

import org.openflow.protocol.OFHello;

public class TranslatorTest extends TestCase {

	OpenVirteXController ctl = null;
	private XidTranslator<OVXSwitch> translator;

	public TranslatorTest(final String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(TranslatorTest.class);
	}

	public void testTranslate() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1, false);

		// make a south-bound message....something simple.
		final OFHello ofh = new OFHello();
		ofh.setXid(0);

		final int newXid = this.translator.translate(ofh.getXid(), vsw);
		Assert.assertEquals(newXid, XidTranslator.MIN_XID);
	}

	public void testUntranslate() {
		final OVXSwitch vsw = new OVXSingleSwitch(1, 1, false);

		final OFHello ofh = new OFHello();
		ofh.setXid(0);
		this.translator.translate(ofh.getXid(), vsw);

		final XidPair<OVXSwitch> pair = this.translator.untranslate(XidTranslator.MIN_XID);
		Assert.assertEquals(pair.getSwitch().getSwitchId(), vsw.getSwitchId());
		Assert.assertEquals(pair.getXid(), ofh.getXid());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
    	ctl = new OpenVirteXController(new CmdLineSettings());
		this.translator = new XidTranslator<OVXSwitch>();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
