package net.onrc.openvirtex.api;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseAPITests {
    public static Test suite() {
	TestSuite suite = new TestSuite(BaseAPITests.class.getName());
	//$JUnit-BEGIN$
	suite.addTest(APITest.suite());
	//$JUnit-END$
	return suite;
    }
}
