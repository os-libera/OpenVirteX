/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.core;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OpenVirteXControllerTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public OpenVirteXControllerTest(final String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static TestSuite suite() {
		return new TestSuite(OpenVirteXControllerTest.class);
	}

	/**
	 * Testing that a call to getInstance on the controller fails with a
	 * RuntimeException and that that the returned object is null.
	 */
	public void testNull() {
		OpenVirteXController ctrl = null;
		try {
			ctrl = OpenVirteXController.getInstance();
		} catch (final RuntimeException e) {
			Assert.assertNull(ctrl);
		}

	}

	public void testNotNull() {
		final OpenVirteXController ovx = new OpenVirteXController(null,
				"localhost", 16633, 8);
		Assert.assertNotNull(OpenVirteXController.getInstance());
		Assert.assertEquals(ovx, OpenVirteXController.getInstance());
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
