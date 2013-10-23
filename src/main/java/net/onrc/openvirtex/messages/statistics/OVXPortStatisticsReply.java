/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;

import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXPortStatisticsReply extends OFPortStatisticsReply 
						implements VirtualizableStatistic {

	private Map<Short, OVXPortStatisticsReply> stats = null;
	
	@Override
	public void virtualizeStatistic(final PhysicalSwitch sw,
			final OVXStatisticsReply msg) {
		stats = new HashMap<Short, OVXPortStatisticsReply>();
		List<? extends OFStatistics> statList = msg.getStatistics();
		for (OFStatistics stat : statList) {
			OVXPortStatisticsReply pStat = (OVXPortStatisticsReply) stat; 
			stats.put(pStat.getPortNumber(), pStat);
		}
		sw.setPortStatistics(stats);
		
	}	
}

