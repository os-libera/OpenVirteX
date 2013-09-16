package net.onrc.openvirtex.api.service.handlers.tenant;

import junit.framework.Test;
import junit.framework.TestSuite;


public class APITests {

	public static Test suite() {
		TestSuite suite = new TestSuite(APITests.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(PassingAPITests.suite());
		//$JUnit-END$
		return suite;
	}
}
