package net.onrc.openvirtex;

import net.onrc.openvirtex.api.service.handlers.tenant.APITests;
import net.onrc.openvirtex.core.BaseCtrlTests;
import net.onrc.openvirtex.elements.BaseMapTests;
import net.onrc.openvirtex.elements.address.BaseIPTests;
import net.onrc.openvirtex.elements.datapath.BaseTranslatorTests;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(BaseCtrlTests.suite());
		suite.addTest(BaseMapTests.suite());
		suite.addTest(BaseIPTests.suite());
		suite.addTest(BaseTranslatorTests.suite());
		suite.addTest(APITests.suite());
		//$JUnit-END$
		return suite;
	}

}
