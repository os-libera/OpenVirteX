package net.onrc.openvirtex.core;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OpenVirteXControllerTest extends TestCase {

    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public OpenVirteXControllerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite()
    {
        return new TestSuite( OpenVirteXControllerTest.class );
    }

    /**
     * Testing that a call to getInstance on the controller
     * fails with a RuntimeException and that that the 
     * returned object is null.
     */
    public void testNull() {
	OpenVirteXController ctrl = null;
	try {
	    ctrl = OpenVirteXController.getInstance();
	} catch (RuntimeException e) {
	    assertNull(ctrl);    
	}
        
    }
    
    public void testNotNull() {
	OpenVirteXController ovx = new OpenVirteXController(null, "localhost", 16633, 8);
	assertNotNull(OpenVirteXController.getInstance());
	assertEquals(ovx, OpenVirteXController.getInstance());
    }
    
    
    protected void setUp() throws Exception {
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

}
