package net.onrc.openvirtex.elements.address;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IPTest extends TestCase {

    private static String DEFAULT = "10.0.0.1";
    
    public IPTest(String name) {
	super(name);
    }

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite()
    {
        return new TestSuite( IPTest.class );
    }
    
    public void testPhysicalIP() {
	PhysicalIPAddress ip = new PhysicalIPAddress(DEFAULT);
	
	assertEquals("PhysicalIPAddress[" + DEFAULT +"]", ip.toString());
    }
    
    public void testVirtualIP() {
	OVXIPAddress ip = new OVXIPAddress(DEFAULT, 0);
	
	assertEquals("OVXIPAddress[" + DEFAULT +"]", ip.toString());
    }
    
    protected void setUp() throws Exception {
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

}
