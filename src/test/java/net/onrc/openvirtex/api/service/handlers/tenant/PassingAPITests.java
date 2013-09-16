package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Collections;




import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import junit.framework.TestSuite;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class PassingAPITests extends AbstractAPICalls {
	
	
	private Integer tid = null;
	
	 /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite()
    {
        return new TestSuite( PassingAPITests.class );
    }
    
    /**
     * Test whether a create network call succeeds
     */
    public void testCreateNetPass() {
    	
    	JSONRPC2Response resp = super.createNetwork();
    	
    	assertNull("CreateOVXNetwork should have not returned null", resp.getError());
    	
    	assertEquals((Integer) 1, resp.getResult());
    	
    	
    	
    }
    
    
    
    /**
     * Test whether the creation of a host actually
     * succeeds.
     */
    public void testCreateSingleSwitch() {
    	PhysicalSwitch sw = new PhysicalSwitch(1);
    	PhysicalNetwork.getInstance().addSwitch(sw);
    	
    	super.createNetwork(); 
    	JSONRPC2Response resp = super.createSwitch(1, Collections.singletonList("1"));
    	System.out.println(resp);
    	assertNull("CreateOVXSwitch should have not returned null", resp.getError());
    	
    	assertEquals((long)1, resp.getResult());
    	
    }
    
    
}
