package net.onrc.openvirtex.core;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseCtrlTests {

    public static Test suite() {
	TestSuite suite = new TestSuite(BaseCtrlTests.class.getName());
	//$JUnit-BEGIN$
	suite.addTest(OpenVirteXControllerTest.suite());
	//$JUnit-END$
	return suite;
    }

}
