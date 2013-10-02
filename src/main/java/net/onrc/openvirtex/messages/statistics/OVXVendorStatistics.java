/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.statistics.OFVendorStatistics;

public class OVXVendorStatistics extends OFVendorStatistics implements
		VirtualizableStatistic, DevirtualizableStatistic {

	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void virtualizeStatistic(final PhysicalSwitch sw,
			final OVXStatisticsReply msg) {
		// TODO Auto-generated method stub

	}

}
