/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.link;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.elements.port.Port;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public abstract class Link<T1 extends Port, T2 extends Switch> implements Persistable {

	Logger       log     = LogManager.getLogger(Link.class.getName());

	public static final String DB_KEY = "links";

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

	@SuppressWarnings("unchecked")
	public T2 getSrcSwitch() {
		return (T2) this.srcPort.getParentSwitch();
	}

	@SuppressWarnings("unchecked")
	public T2 getDstSwitch() {
		return (T2) this.dstPort.getParentSwitch();
	}

	@Override
	public String toString() {
		final String srcSwitch = this.getSrcSwitch().getSwitchId()
				.toString();
		final String dstSwitch = this.getDstSwitch().getSwitchId()
				.toString();
		final short srcPort = this.srcPort.getPortNumber();
		final short dstPort = this.dstPort.getPortNumber();
		return srcSwitch + ":" + srcPort + "-" + dstSwitch + ":" + dstPort;
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

	public boolean equals(final Link link) {
		if (link.dstPort.equals(this.dstPort)
				&& link.srcPort.equals(this.srcPort)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Removes mappings and dependencies related to this link.
	 */
	public abstract void unregister();

	/**
	 * Compute the link metric based on the link nominal throughput, like OSPF
	 * Formula is => metric = refBandwidth / linkThroughput, where refBandwidth
	 * is 100Gbps.
	 * If the ports expose different throughputs, trigger a warning and assume a
	 * metric
	 * of 1000 (100Mbps)
	 * 
	 * @return the link metric
	 */
	public Integer getMetric() {
		// System.out.println("Implement link throughput in Link.java!!!!");

		if (this.srcPort.getCurrentThroughput().equals(
				this.dstPort.getCurrentThroughput())) {
			// Throughput is expressed in Mbps.
			this.log.debug("Metric for link between {}-{},{}-{} is {}",
					this.getSrcSwitch().getSwitchName(),
					this.srcPort.getPortNumber(),
					this.getDstSwitch().getSwitchName(),
					this.dstPort.getPortNumber(),
					100000 / this.srcPort.getCurrentThroughput());
			return 100000 / this.srcPort.getCurrentThroughput();
		} else {
			this.log.warn(
					"getMetric: ports have different throughput. Source: {}-{} = {}, Destination: {}-{} = {}",
					this.getSrcSwitch().getSwitchName(),
					this.srcPort.getPortNumber(),
					this.srcPort.getCurrentThroughput(),
					this.getDstSwitch().getSwitchName(),
					this.dstPort.getPortNumber(),
					this.dstPort.getCurrentThroughput());
			return 1000;
		}
	}

	@Override
	public Map<String, Object> getDBIndex() {
		return null;
	}

	@Override
	public String getDBKey() {
		return Link.DB_KEY;
	}

	@Override
	public String getDBName() {
		return null;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.put(TenantHandler.SRC_DPID, this.srcPort.getParentSwitch().getSwitchId());
		dbObject.put(TenantHandler.SRC_PORT, this.srcPort.getPortNumber());
		dbObject.put(TenantHandler.DST_DPID, this.dstPort.getParentSwitch().getSwitchId());
		dbObject.put(TenantHandler.DST_PORT, this.dstPort.getPortNumber());
		return dbObject;
	}	
}
