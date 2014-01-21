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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFPhysicalPort;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

public class MapAddTest extends TestCase {

	private final int MAXIPS = 1000;
	private final int MAXTIDS = 10;

	private final int MAXPSW = 1000;

	@SuppressWarnings("unused")
	private OpenVirteXController ctl = null; 
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

	public void testAddLinks() {
		PhysicalNetwork pn = PhysicalNetwork.getInstance();
		Map<Long, PhysicalSwitch> pswmap = new HashMap<Long, PhysicalSwitch>(); /*DPID, PSW*/
		List<OVXSwitch> vswmap = new ArrayList<OVXSwitch>(); /*TID, VSW*/
		makeSwitches(pswmap, vswmap, pn);

		OVXLink vlink = null;
		PhysicalLink plink = null;
		SwitchRoute route;
		/* create *Links and routes, add to map. Premise is a fully-connected 
		 * network of five nodes, with 1) a 1:1 Physical to OVXNetwork mapping, 
		 * and 2) a single BVS over the fully-connected physical network */
		try {
			for (PhysicalSwitch psrc : pswmap.values()) {
				OVXSwitch vsrc = this.map.getVirtualSwitch(psrc, 1);
				for (PhysicalSwitch pdst : pswmap.values()) {
					OVXSwitch vdst = this.map.getVirtualSwitch(pdst, 1);
					for (PhysicalPort srcp : psrc.getPorts().values()) {
						OVXPort vsrcp = vsrc.getPort(srcp.getPortNumber());
						for (PhysicalPort dstp : pdst.getPorts().values()) {
							if (srcp.equals(dstp)) {
								continue;
							}
							OVXPort vdstp = vdst.getPort(dstp.getPortNumber());
							plink = new PhysicalLink(srcp, dstp);
							try {
								vlink = new OVXLink((int)dstp.getPortNumber(), 1, vsrcp, vdstp, 
										new RoutingAlgorithms("none", (byte)0));
							} catch (PortMappingException e) {
								fail();
							}
							route = new SwitchRoute(vsrc, vsrcp, vdstp, 
									1, (byte)0xf);

							/* Add to mapping, verify we get back what we placed*/
							this.map.addLinks(Collections.singletonList(plink), vlink);
							Assert.assertEquals(vlink, this.map.getVirtualLinks(plink, 1));
							Assert.assertEquals(Collections.singletonList(plink), 
									this.map.getPhysicalLinks(vlink));
							this.map.addRoute(route, Collections.singletonList(plink));
							Assert.assertEquals(route, this.map.getSwitchRoutes(plink, 1));
							Assert.assertEquals(Collections.singletonList(plink), 
									this.map.getRoute(route));
						}
					}
				}
			}
		} catch (SwitchMappingException | LinkMappingException | RoutingAlgorithmException e) {
			/* silently ignore for now */
		}
	}

	/**
	 * Creates a set of Physical and OVX switches. 
	 * @param pswmap
	 * @param vswmap
	 * @param pn
	 */
	protected void makeSwitches(Map<Long, PhysicalSwitch> pswmap, 
			List<OVXSwitch> vswmap, PhysicalNetwork pn) {
		PhysicalSwitch psw = null;
		PhysicalPort pport = null;
		OVXSwitch vsw = null;
		OVXPort vport = null;

		/* make 5 PSWs with 5 ports */
		for (long i = 0; i < this.MAXTIDS/2; i++) {
			psw = new PhysicalSwitch(i); /*DPID*/
			for (int j = 0; j < this.MAXTIDS/2; j++) {
				pport = this.makePhyPort((short)j, psw);
				psw.addPort(pport);
			}
			pswmap.put(i, psw);
		}
		/* make 5 VSWs for a tenant */
		for (long j = 0; j < this.MAXTIDS/2; j++) {
			vsw = new OVXSingleSwitch(j, 1); /*DPID, TID*/
			vswmap.add(vsw);
		}

		/* add ports to VSW 1:1 physical to virtual */
		for (OVXSwitch v : vswmap) {
			psw = pswmap.get(v.getSwitchId());
			this.map.addSwitches(Collections.singletonList(psw), v);
			for (PhysicalPort p : psw.getPorts().values()) {
				try {
					vport = new OVXPort(1, p, false, p.getPortNumber());
					v.addPort(vport);
				} catch (IndexOutOfBoundException e) {
					continue;
				}
			}
		}
	}

	protected PhysicalPort makePhyPort(short portnum, PhysicalSwitch psw) {
		OFPhysicalPort ofpp = new OFPhysicalPort();
		ofpp.setPortNumber(portnum);
		/* whether edge or not doesn't matter for us */
		return new PhysicalPort(ofpp, psw, false);
	}

	protected OVXPort makeOVXPort(short portnum, final int tenant, final PhysicalPort port) {
		try {
			/* whether edge or not doesn't matter for us */
			return new OVXPort(tenant, port, false, portnum);
		} catch (IndexOutOfBoundException e) {
			return null;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.map = OVXMap.getInstance();
		this.ctl = new OpenVirteXController(new CmdLineSettings());

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
