package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

public interface DevirtualizableStatistic {
	/**
	 * 
	 * Devirtualize a statistics object.
	 * 
	 * @param sw
	 *            Switch which sent the object
	 * @param msg
	 *            the actual statistics message.
	 */
	public void devirtualizeStatistic(OVXSwitch sw, OVXStatisticsRequest msg);

}
