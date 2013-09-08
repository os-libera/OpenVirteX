package net.onrc.openvirtex.elements.datapath;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BaseTranslatorTests {

    public static Test suite() {
	TestSuite suite = new TestSuite(BaseTranslatorTests.class.getName());
	//$JUnit-BEGIN$
	suite.addTest(TranslatorTest.suite());
	//$JUnit-END$
	return suite;
    }

}
