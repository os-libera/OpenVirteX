/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;

import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXFlowStatisticsReply extends OFFlowStatisticsReply implements
		VirtualizableStatistic {
	
	
	@Override
	public void virtualizeStatistic(final PhysicalSwitch sw,
			final OVXStatisticsReply msg) {
		if (msg.getXid() != 0) {
			sw.removeFlowMods(msg);
			return;
		}
		
		HashMap<Integer, List<OVXFlowStatisticsReply>> stats = new HashMap<Integer, List<OVXFlowStatisticsReply>>();
		
		for (OFStatistics stat : msg.getStatistics()) {
			OVXFlowStatisticsReply reply = (OVXFlowStatisticsReply) stat;
			int tid = getTidFromCookie(reply.getCookie());
			addToStats(tid, reply, stats);
		}
		sw.setFlowStatistics(stats);
	}

	private void addToStats(int tid, OVXFlowStatisticsReply reply,
			HashMap<Integer, List<OVXFlowStatisticsReply>> stats) {
		List<OVXFlowStatisticsReply> statsList = stats.get(tid);
		if (statsList == null) 
			statsList = new LinkedList<OVXFlowStatisticsReply>();
		statsList.add(reply);
		stats.put(tid, statsList);
	}

	private int getTidFromCookie(long cookie) {
		return (int) (cookie >> 32);
	}

}
