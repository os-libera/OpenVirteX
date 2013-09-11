package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.messages.OVXFlowMod;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FlowTableTest extends TestCase {
	
    public FlowTableTest(String name) {
	super(name);
    }
    
    public static Test suite() {
	return new TestSuite( FlowTableTest.class );
    }

    public void testAddFlowMod() {
	OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	OVXFlowTable oft = new OVXFlowTable(vsw);
	OVXFlowMod fm1 = new OVXFlowMod();
	
	long c1 = (((long)vsw.getTenantId() << 32) | 1);
	long c = oft.addFlowMod(fm1);
	
	assertEquals(c, c1);
    }
    
    public void testDeleteFlowMod() {
	OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	OVXFlowTable oft = new OVXFlowTable(vsw);
	OVXFlowMod fm1 = new OVXFlowMod();
	
	long c = oft.addFlowMod(fm1);
	OVXFlowMod fm2 = oft.deleteFlowMod(c);
	
	assertEquals(fm1, fm2);
    }
    
    public void testGenerateCookie() {
	OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	OVXFlowTable oft = new OVXFlowTable(vsw);
	
	OVXFlowMod fm1 = new OVXFlowMod();
	OVXFlowMod fm2 = new OVXFlowMod();	
	OVXFlowMod fm3 = new OVXFlowMod();
	
	long c1 = (((long)vsw.getTenantId() << 32) | 1);
	long c2 = (((long)vsw.getTenantId() << 32) | 2);
	
	//generate new cookies while none in freelist
	long c = oft.addFlowMod(fm1);
	assertEquals(c, c1);
	c = oft.addFlowMod(fm2);
	assertEquals(c, c2);
	
	//should re-use first cookie that was freed up
	oft.deleteFlowMod(c1);
	c = oft.addFlowMod(fm3);
	assertEquals(c, c1);	
    }

    protected void setUp() throws Exception {
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

}
