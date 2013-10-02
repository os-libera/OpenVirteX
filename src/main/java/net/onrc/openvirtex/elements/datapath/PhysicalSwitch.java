/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.Virtualizable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;

/**
 * The Class PhysicalSwitch.
 */
public class PhysicalSwitch extends Switch<PhysicalPort> {

	/** The log. */
	Logger log = LogManager.getLogger(PhysicalSwitch.class.getName());

	/** The Xid mapper */
	private final XidTranslator translator;

	/**
	 * Instantiates a new physical switch.
	 * 
	 * @param switchId
	 *            the switch id
	 */
	public PhysicalSwitch(final long switchId) {
		super(switchId);
		this.translator = new XidTranslator();
	}

	/**
	 * Gets the OVX port number.
	 * 
	 * @param physicalPortNumber
	 *            the physical port number
	 * @param tenantId
	 *            the tenant id
	 * @return the oVX port number
	 */
	public Short getOVXPortNumber(final Short physicalPortNumber,
			final Integer tenantId, final Integer vLinkId) {
		return this.portMap.get(physicalPortNumber)
				.getOVXPort(tenantId, vLinkId).getPortNumber();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
	 * .OFMessage)
	 */
	@Override
	public void handleIO(final OFMessage msg) {
		try {
			((Virtualizable) msg).virtualize(this);
		} catch (final ClassCastException e) {
			this.log.error("Received illegal message : " + msg);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
	 */
	@Override
	public void tearDown() {
		this.log.info("Switch disconnected {} ",
				this.featuresReply.getDatapathId());
		this.channel.disconnect();

	}

	/**
	 * Fill port map. Assume all ports are edges until discovery says otherwise.
	 */
	protected void fillPortMap() {
		for (final OFPhysicalPort port : this.featuresReply.getPorts()) {
			final PhysicalPort physicalPort = new PhysicalPort(port, this, true);
			this.addPort(physicalPort);
		}
	}

	@Override
	public boolean addPort(final PhysicalPort port) {
		final boolean result = super.addPort(port);
		if (result) {
			PhysicalNetwork.getInstance().addPort(port);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.onrc.openvirtex.elements.datapath.Switch#init()
	 */
	@Override
	public boolean boot() {
		PhysicalNetwork.getInstance().addSwitch(this);
		this.log.info("Switch connected {} : {}",
				this.featuresReply.getDatapathId(),
				this.desc.getHardwareDescription());
		this.fillPortMap();
		return true;
	}

	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		if (this.isConnected) {
			this.channel.write(Collections.singletonList(msg));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
	 */
	@Override
	public String toString() {

		return "DPID : " + this.featuresReply.getDatapathId()
				+ ", remoteAddr : "
				+ this.channel.getRemoteAddress().toString();
	}

	/**
	 * Gets the port.
	 * 
	 * @param portNumber
	 *            the port number
	 * @return the port instance
	 */
	@Override
	public PhysicalPort getPort(final Short portNumber) {
		return this.portMap.get(portNumber);
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof PhysicalSwitch) {
			return this.switchId == ((PhysicalSwitch) other).switchId;
		}

		return false;
	}

	public int translate(final OFMessage ofm, final OVXSwitch sw) {
		return this.translator.translate(ofm.getXid(), sw);
	}

	public XidPair untranslate(final OFMessage ofm) {
		final XidPair pair = this.translator.untranslate(ofm.getXid());
		if (pair == null) {
			return null;
		}
		return pair;
	}
}
