/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;

public interface VirtualizableStatistic {
	/**
	 * 
	 * Virtualize a statistics object.
	 * 
	 * @param sw
	 *            Switch which received the object
	 * @param msg
	 *            the actual statistics message.
	 */
	public void virtualizeStatistic(PhysicalSwitch sw, OVXStatisticsReply msg);

}
