package net.onrc.openvirtex;

import net.onrc.openvirtex.core.BaseCtrlTests;
import net.onrc.openvirtex.elements.BaseMapTests;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
	TestSuite suite = new TestSuite(AllTests.class.getName());
	//$JUnit-BEGIN$
	suite.addTest(BaseCtrlTests.suite());
	suite.addTest(BaseMapTests.suite());
	//$JUnit-END$
	return suite;
    }

}
