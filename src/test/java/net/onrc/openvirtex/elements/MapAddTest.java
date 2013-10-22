/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.util.MACAddress;

public class MapAddTest extends TestCase {

	private final int MAXIPS = 1000;
	private final int MAXTIDS = 10;

	private final int MAXPSW = 1000;

	private Mappable map = null;

	public MapAddTest(final String name) {
		super(name);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static TestSuite suite() {
		return new TestSuite(MapAddTest.class);
	}

	public void testAddIP() {
		for (int i = 0; i < this.MAXIPS; i++) {
			for (int j = 0; j < this.MAXTIDS; j++) {
				this.map.addIP(new PhysicalIPAddress(i), new OVXIPAddress(j, i));
			}
		}
		try {
			for (int i = 0; i < this.MAXIPS; i++) {
				for (int j = 0; j < this.MAXTIDS; j++) {
			        	Assert.assertEquals(
	                            		this.map.getVirtualIP(new PhysicalIPAddress(i)),
	                            		new OVXIPAddress(j, i));
                            
				}
			}
		} catch (AddressMappingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }	
	}

	public void testAddSwitches() {
		final ArrayList<PhysicalSwitch> p_sw = new ArrayList<PhysicalSwitch>();
		final ArrayList<OVXSwitch> v_sw = new ArrayList<OVXSwitch>();
		PhysicalSwitch sw = null;
		OVXSwitch vsw = null;
		for (int i = 0; i < this.MAXPSW; i++) {
			sw = new PhysicalSwitch(i);
			p_sw.add(sw);
			for (int j = 0; j < this.MAXTIDS; j++) {
				vsw = new OVXSingleSwitch(i, j);
				v_sw.add(vsw);
				this.map.addSwitches(Collections.singletonList(sw), vsw);
			}
		}
		try {
			for (int i = 0; i < this.MAXPSW; i++) {
				for (int j = 0; j < this.MAXTIDS; j++) {
					Assert.assertEquals(this.map.getVirtualSwitch(p_sw.get(i), j),
						v_sw.remove(0));
				}
			}
		} catch (SwitchMappingException e) {
			e.printStackTrace();
		}
	}

	public void testAddMacs() {
		for (int i = 0; i < this.MAXPSW; i++) {
			this.map.addMAC(MACAddress.valueOf(i), i % this.MAXTIDS);
		}
		try {
			for (int i = 0; i < this.MAXPSW; i++) {
				Assert.assertEquals((int) this.map.getMAC(MACAddress.valueOf(i)), i
	                    		% this.MAXTIDS);
                	}
		} catch (AddressMappingException e) {
                   	e.printStackTrace();
                }

	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.map = OVXMap.getInstance();

	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();
	}

}
