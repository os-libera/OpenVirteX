/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.address;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IPTest extends TestCase {

	private static String DEFAULT = "10.0.0.1";

	public IPTest(final String name) {
		super(name);
	}

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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
