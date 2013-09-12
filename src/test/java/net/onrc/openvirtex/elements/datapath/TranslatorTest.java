package net.onrc.openvirtex.elements.datapath;

import org.openflow.protocol.OFHello;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TranslatorTest extends TestCase {

    private XidTranslator translator;
    
    public TranslatorTest(String name) {
	super(name);
    }
    
    public static Test suite() {
	return new TestSuite( TranslatorTest.class );
    }
    
    public void testTranslate() {
	OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	
	//make a south-bound message....something simple.
	OFHello ofh = new OFHello();
	ofh.setXid(0);
	
	int newXid = this.translator.translate(ofh.getXid(), vsw);
	assertEquals(newXid, XidTranslator.MIN_XID);
    }
    
    public void testUntranslate() {
	OVXSwitch vsw = new OVXSingleSwitch(1, 1);
	
	OFHello ofh = new OFHello();
	ofh.setXid(0);
	this.translator.translate(ofh.getXid(), vsw);
	
	XidPair pair = this.translator.untranslate(XidTranslator.MIN_XID);
	assertEquals(pair.getSwitch().getSwitchId(), vsw.getSwitchId());
	assertEquals(pair.getXid(), ofh.getXid());
    }
        
    protected void setUp() throws Exception {
	this.translator = new XidTranslator();
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

}
