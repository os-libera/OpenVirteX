package net.onrc.openvirtex.api;

import java.util.ArrayList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class APITest extends TestCase{

    private APITenantManager tenantManager;
    
    public APITest(String name) {
	super(name);
    }
    
    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite( APITest.class );
    }
    
    protected void setUp() throws Exception {
	super.setUp();
	this.tenantManager = new APITenantManager();
    }
    
    protected void tearDown() throws Exception {
	super.tearDown();
    }
    
    // Testing API functionality
    public void testCreateNetwork() {
	short mask = 8;
	int tenantId = 0;
        try {
            // create first test network. the tenant id should be equal to 1
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.0.5.10", 6633, "10.0.0.1", mask);
	    assertEquals(tenantId, 1);
            // create first test network. the tenant id should be equal to 2
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.10.5.10", 6635, "10.0.0.1", mask);
	    assertEquals(tenantId, 2);
        } catch (ControllerUnavailableException e) {
            fail("Controller UnavailableException occured even though it should not have happened.");
        }        
    }
    
    public void testCreateSwitch() {
	short mask = 8;
	int tenantId = 0;
	OVXMap map = OVXMap.getInstance();
	try {
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.0.0.10", 6633, "10.0.0.1", mask);
	    PhysicalNetwork.getInstance().addSwitch(new PhysicalSwitch(1));
	    PhysicalNetwork.getInstance().addSwitch(new PhysicalSwitch(2));
	    PhysicalNetwork.getInstance().addSwitch(new PhysicalSwitch(3));
	    
	    List <String> dpids = new ArrayList<String>();
	    
	    // add a virtual switch corresponding to the physicalSwitch with dpid 1
	    dpids.add("1");
	    this.tenantManager.createOVXSwitch(tenantId, dpids);
	    // add a virtual switch corresponding to the physicalSwitch with dpid 2
	    dpids.remove(0);
	    dpids.add("2");
	    this.tenantManager.createOVXSwitch(tenantId, dpids);
	    
	    // add a virtual switch corresponding to the physicalSwitch with dpid 3
	    dpids.remove(0);
	    dpids.add("3");
	    this.tenantManager.createOVXSwitch(tenantId, dpids);
        } catch(InvalidDPIDException e) {
            fail("DPID we have given is correct.. should not throw exception");
        } catch (ControllerUnavailableException e) {
            fail("Controller UnavailableException occured even though it should not have happened.");
        } catch (InvalidTenantIdException e) {
            fail("Tenant ID is correct.. should not throw exception");
        }
	
    }
    
    public void testCreateLink() {
	// TODO: a test which creates links 
    }
    
    public void testAddHost() {
	// TODO: a test which adds a host to the network
    }
    
    public void testBootNetwork() {
	short mask = 8;
	int tenantId;
        try {
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.0.9.10", 6633, "10.0.0.1", mask);
	    this.tenantManager.bootNetwork(tenantId);
        } catch (ControllerUnavailableException e) {
            fail("controller should be available");
        } catch (InvalidTenantIdException e) {
	    fail("tenantId should be valid");
        }
	
    }
    
    // Testing 
    
    public void testInvalidTenantId() {
	boolean failed = false;
	int tenantId = 0;
	short mask = 8;
	try {
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.9.9.10", 6633, "10.0.0.1", mask);
	    this.tenantManager.bootNetwork(tenantId+1);
	} catch (ControllerUnavailableException e) {
	    fail("controller should be available");
        } catch (InvalidTenantIdException e) {
	    failed = true;
        }
	assertTrue(failed);
    }

    public void testInvalidController() {
	boolean failed = false;
	int tenantId = 0;
	short mask = 8;
	try {
	    tenantId = this.tenantManager.createOVXNetwork("http", "192.9.9.10", 6633, "10.0.0.1", mask);
	} catch (ControllerUnavailableException e) {
	    failed = true;
        }
	assertTrue(failed);
    }
    
    public void testInvalidDPID() {
	// TODO: check that the dpid error gets thrown if we try to add dpid to two virtual switches in same virtual network
    }
    
    public void testInvalidEdgePort() {
	// TODO: check that the port error is thrown if port specified is not an edge port
    }
    
    public void testInvalidLink() {
	// TODO: check the link that is created is unique and exists on the physical plane 
    }
}
