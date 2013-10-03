/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.onrc.openvirtex.api.service.handlers.tenant.APITests;
import net.onrc.openvirtex.core.BaseCtrlTests;
import net.onrc.openvirtex.elements.BaseMapTests;
import net.onrc.openvirtex.elements.address.BaseIPTests;
import net.onrc.openvirtex.elements.datapath.BaseTranslatorTests;

public class AllTests {

	public static Test suite() {
		final TestSuite suite = new TestSuite(AllTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(BaseCtrlTests.suite());
		suite.addTest(BaseMapTests.suite());
		suite.addTest(BaseIPTests.suite());
		suite.addTest(BaseTranslatorTests.suite());
		suite.addTest(APITests.suite());
		// $JUnit-END$
		return suite;
	}

}
