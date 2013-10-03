/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.linkdiscovery.SwitchDiscoveryManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.openflow.protocol.OFMessage;

/**
 * 
 * Singleton class for physical network. Maintains SwitchDiscoveryManager for
 * each switch in the physical network. Listens for LLDP packets and passes them
 * on to the appropriate SwitchDiscoveryManager. Creates and maintains links
 * after discovery, and switch ports are made discoverable here.
 * 
 * TODO: should probably subscribe to PORT UP/DOWN events here
 * 
 */
public class PhysicalNetwork extends
		Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

	private static PhysicalNetwork instance;
	private ArrayList<Uplink> uplinkList;
	private final Map<Long, SwitchDiscoveryManager> discoveryManager;
	private static HashedWheelTimer timer;
	static Logger log = LogManager.getLogger(PhysicalNetwork.class.getName());

	private PhysicalNetwork() {
		PhysicalNetwork.log.info("Starting network discovery...");
		PhysicalNetwork.timer = new HashedWheelTimer();
		this.discoveryManager = new HashMap<Long, SwitchDiscoveryManager>();
	}

	public static PhysicalNetwork getInstance() {
		if (PhysicalNetwork.instance == null) {
			PhysicalNetwork.instance = new PhysicalNetwork();
		}
		return PhysicalNetwork.instance;
	}

	public static HashedWheelTimer getTimer() {
		return PhysicalNetwork.timer;
	}

	public static void reset() {
		PhysicalNetwork.log
				.debug("PhysicalNetwork has been explicitely reset. Hope you know what you are doing!!");
		PhysicalNetwork.instance = null;
	}

	public ArrayList<Uplink> getUplinkList() {
		return this.uplinkList;
	}

	public void setUplinkList(final ArrayList<Uplink> uplinkList) {
		this.uplinkList = uplinkList;
	}

	/**
	 * Add switch to topology and make discoverable
	 */
	@Override
	public synchronized void addSwitch(final PhysicalSwitch sw) {
		super.addSwitch(sw);
		this.discoveryManager.put(sw.getSwitchId(), new SwitchDiscoveryManager(
				sw));
	}

	/**
	 * Add port for discovery
	 * 
	 * @param port
	 */
	public synchronized void addPort(final PhysicalPort port) {
		this.discoveryManager.get(port.getParentSwitch().getSwitchId())
				.addPort(port);
	}

	/**
	 * Remove port from discovery
	 * 
	 * @param port
	 */
	public synchronized void removePort(final PhysicalPort port) {
		this.discoveryManager.get(port.getParentSwitch().getSwitchId())
				.removePort(port);
	}
	
	/**
	 * Create link and add it to the topology.
	 * 
	 * @param srcPort
	 * @param dstPort
	 */
	public synchronized void createLink(final PhysicalPort srcPort,
			final PhysicalPort dstPort) {
		final PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
		if (neighbourPort == null || !neighbourPort.equals(dstPort)) {
			final PhysicalLink link = new PhysicalLink(srcPort, dstPort);
			super.addLink(link);
		} else {
			PhysicalNetwork.log.debug("Tried to create invalid link");
		}
	}

	/**
	 * Create link and add it to the topology.
	 * 
	 * @param srcPort
	 * @param dstPort
	 */
	public synchronized void removeLink(final PhysicalPort srcPort,
			final PhysicalPort dstPort) {
		final PhysicalPort neighbourPort = this.getNeighborPort(srcPort);
		if (neighbourPort.equals(dstPort)) {
			final PhysicalLink link = super.getLink(srcPort, dstPort);
			super.removeLink(link);
		} else {
			this.log.debug("Tried to remove invalid link");
		}
	}

	/**
	 * Acknowledge reception of discovery probe to sender port
	 * 
	 * @param port
	 */
	public void ackProbe(final PhysicalPort port) {
		final SwitchDiscoveryManager sdm = this.discoveryManager.get(port
				.getParentSwitch().getSwitchId());
		if (sdm != null) {
			sdm.ackProbe(port);
		}
	}

	/**
	 * Handle LLDP packets by passing them on to the appropriate
	 * SwitchDisoveryManager (which sent the original LLDP packet).
	 */
	@Override
	public void handleLLDP(final OFMessage msg, final Switch sw) {
		// Pass msg to appropriate SwitchDiscoveryManager
		final SwitchDiscoveryManager sdm = this.discoveryManager.get(sw
				.getSwitchId());
		if (sdm != null) {
			sdm.handleLLDP(msg, sw);
		}
	}

	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		// Do nothing
	}

	@Override
	public String getName() {
		return "Physical network";
	}

	@Override
	public boolean boot() {
		return true;
	}

}
