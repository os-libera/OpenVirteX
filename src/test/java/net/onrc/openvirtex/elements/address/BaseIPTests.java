/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.address;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseIPTests {

	public static Test suite() {
		final TestSuite suite = new TestSuite(BaseIPTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(IPTest.suite());
		// $JUnit-END$
		return suite;
	}

}
