package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.util.MACAddress;

public class MapAddTest extends TestCase {

    
    private int MAXIPS = 1000;
    private int MAXTIDS = 10;
    
    private int MAXPSW = 1000;
    
    private Mappable map = null;
    
    public MapAddTest(String name) {
	super(name);
    }
    
    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite()
    {
        return new TestSuite( MapAddTest.class );
    }
    
    
    public void testAddIP() {
	for (int i = 0 ; i < MAXIPS ; i++)
	    for (int j = 0 ; j < MAXTIDS ; j++)
		map.addIP(new PhysicalIPAddress(i), new OVXIPAddress(j, i));
	
	for (int i = 0 ; i < MAXIPS ; i++)
	    for (int j = 0 ; j < MAXTIDS ; j++)
		assertEquals(map.getVirtualIP(new PhysicalIPAddress(i)), new OVXIPAddress(j, i));
	
    }
    
    
    public void testAddSwitches() {
	ArrayList<PhysicalSwitch> p_sw = new ArrayList<>();
	ArrayList<OVXSwitch> v_sw = new ArrayList<>();
	PhysicalSwitch sw = null;
	OVXSwitch vsw = null;
	for (int i = 0 ; i < MAXPSW ; i++) {
	    sw = new PhysicalSwitch(i);
	    p_sw.add(sw);
	    for (int j = 0 ; j < MAXTIDS ; j++) {
	    	vsw = new OVXSingleSwitch(i, j);
	    	v_sw.add(vsw);
	    	map.addSwitches(Collections.singletonList(sw), vsw);
	    }
	}
	
	for (int i = 0 ; i < MAXPSW ; i++) {
	    for (int j = 0 ; j < MAXTIDS ; j++) {
		assertEquals(map.getVirtualSwitch(p_sw.get(i), j), v_sw.remove(0));
	    }
	}
	
	
    }
    
    public void testAddMacs() {
	for (int i = 0 ; i < MAXPSW ; i++) 
	    map.addMAC(MACAddress.valueOf(i), i % MAXTIDS);
	
	for (int i = 0 ; i < MAXPSW ; i++)
	    assertEquals((int) map.getMAC(MACAddress.valueOf(i)), (int) (i %  MAXTIDS));
	
    }
    
    
    
    
    

    protected void setUp() throws Exception {
	super.setUp();
	map = OVXMap.getInstance();
	
    }

    protected void tearDown() throws Exception {
	
	super.tearDown();
    }

}
