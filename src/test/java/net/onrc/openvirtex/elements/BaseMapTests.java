package net.onrc.openvirtex.elements;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseMapTests {

	public static Test suite() {
		final TestSuite suite = new TestSuite(BaseMapTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(MapAddTest.suite());
		// $JUnit-END$
		return suite;
	}

}
