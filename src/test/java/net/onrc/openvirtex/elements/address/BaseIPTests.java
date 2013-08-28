package net.onrc.openvirtex.elements.address;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseIPTests {

    public static Test suite() {
	TestSuite suite = new TestSuite(BaseIPTests.class.getName());
	//$JUnit-BEGIN$
	suite.addTest(IPTest.suite());
	//$JUnit-END$
	return suite;
    }

}
