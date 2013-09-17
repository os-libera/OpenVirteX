package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Collections;




import java.util.LinkedList;
import java.util.List;

import org.openflow.protocol.OFPhysicalPort;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import junit.framework.TestSuite;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class PassingAPITests extends AbstractAPICalls {



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

		assertNull("CreateOVXNetwork should not return null", resp.getError());

		assertEquals((long)1, resp.getResult());

	}

	/**
	 * Test whether the creation of a host actually
	 * succeeds.
	 */
	public void testCreateBigSwitch() {
		PhysicalSwitch sw1 = new PhysicalSwitch(1);
		PhysicalSwitch sw2 = new PhysicalSwitch(2);
		PhysicalNetwork.getInstance().addSwitch(sw1);
		PhysicalNetwork.getInstance().addSwitch(sw2);
		super.createNetwork(); 
		List<String> l = new LinkedList<>();
		l.add("1");
		l.add("2");
		JSONRPC2Response resp = super.createSwitch(1, l);

		assertNull("CreateOVXSwitch should have not returned null", resp.getError());

		assertEquals((long)1, resp.getResult());

	}

	public void testConnectHost() {
		super.createNetwork();
		PhysicalSwitch sw1 = new PhysicalSwitch(1);
		PhysicalNetwork.getInstance().addSwitch(sw1);
		super.createSwitch(1, Collections.singletonList("1"));
		PhysicalPort port = new PhysicalPort(new OFPhysicalPort(), sw1, true);
		port.setHardwareAddress(new byte[] {0x01,0x02,0x03,0x04,0x05,0x06});
		sw1.addPort(port);
		
		
		JSONRPC2Response resp = super.connectHost(1, (long) 1, (short)0, "00:00:00:00:00:01");

		assertNull(resp.getError()==null ? 
				"ConnectHost should not return null" : resp.getError().getMessage(),  
				resp.getError());

		assertEquals((short)1, resp.getResult());


	}
	
	public void testCreateLink() {
		super.createNetwork();
		PhysicalSwitch sw1 = new PhysicalSwitch(1);
		PhysicalSwitch sw2 = new PhysicalSwitch(2);
		PhysicalNetwork.getInstance().addSwitch(sw1);
		PhysicalNetwork.getInstance().addSwitch(sw2);
		
		super.createSwitch(1, Collections.singletonList("1"));
		super.createSwitch(1, Collections.singletonList("2"));
		
		PhysicalPort p1 = new PhysicalPort(new OFPhysicalPort(), sw1, false);
		p1.setHardwareAddress(new byte[] {0x01,0x02,0x03,0x04,0x05,0x06});
		sw1.addPort(p1);
		
		PhysicalPort p2 = new PhysicalPort(new OFPhysicalPort(), sw2, false);
		p2.setHardwareAddress(new byte[] {0x11,0x12,0x13,0x14,0x15,0x16});
		sw2.addPort(p2);
		
		PhysicalNetwork.getInstance().createLink(p1, p2);
		
		JSONRPC2Response resp = super.createLink(1, "1/0-2/0");
		
		assertNull(resp.getError()==null ? 
				"CreatLink should not return null" : resp.getError().getMessage(),  
				resp.getError());
		
		assertEquals(2, resp.getResult());
		
		
	}


	@Override
	protected void tearDown() throws Exception {
		OVXMap.reset();
		PhysicalNetwork.reset();
		OVXNetwork.reset();
	}




}
