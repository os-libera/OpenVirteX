/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.link;

import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The Class Link.
 * 
 * @param <T1>
 *            the generic type (Port)
 * @param <T2>
 *            the generic type (Switch)
 */
public abstract class Link<T1, T2> {

	/** The source port. */
	@SerializedName("src")
	@Expose
	protected T1 srcPort = null;

	/** The destination port. */
	@SerializedName("dst")
	@Expose
	protected T1 dstPort = null;

	/**
	 * Instantiates a new link.
	 * 
	 * @param srcPort
	 *            the source port instance
	 * @param dstPort
	 *            the destination port instance
	 */
	protected Link(final T1 srcPort, final T1 dstPort) {
		super();
		this.srcPort = srcPort;
		this.dstPort = dstPort;
	}

	/**
	 * Gets the source port instance.
	 * 
	 * @return the source port
	 */
	public T1 getSrcPort() {
		return this.srcPort;
	}

	/**
	 * Gets the destination port instance.
	 * 
	 * @return the destination port
	 */
	public T1 getDstPort() {
		return this.dstPort;
	}

	@SuppressWarnings("unchecked")
	public T2 getSrcSwitch() {
		return (T2) ((Port) this.srcPort).getParentSwitch();
	}

	@SuppressWarnings("unchecked")
	public T2 getDstSwitch() {
		return (T2) ((Port) this.dstPort).getParentSwitch();
	}

	@Override
	public String toString() {
		final String srcSwitch = ((Switch) this.getSrcSwitch()).getSwitchId()
				.toString();
		final String dstSwitch = ((Switch) this.getDstSwitch()).getSwitchId()
				.toString();
		final short srcPort = ((Port) this.srcPort).getPortNumber();
		final short dstPort = ((Port) this.dstPort).getPortNumber();
		return srcSwitch + ":" + srcPort + "-" + dstSwitch + ":" + dstPort;
	}

	public boolean equals(final Link link) {
		if (link.dstPort.equals(this.dstPort)
				&& link.srcPort.equals(this.srcPort)) {
			return true;
		} else {
			return false;
		}
	}
}
