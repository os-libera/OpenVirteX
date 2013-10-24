/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import java.util.Collections;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;

public class OVXTableStatistics extends OFTableStatistics implements
		VirtualizableStatistic, DevirtualizableStatistic {

	
	/*
	 * TODO
	 * Ideally, this would get information about the real flowtables
	 * and aggregate them in some smart way. This probably needs to 
	 * be discussed with the overall OVX team
	 */
	
	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		this.activeCount = sw.getFlowTable().getFlowTable().size();
		this.tableId = 1;
		/*
		 * FIXME
		 * Currently preventing controllers from wildcarding the IP
		 * field. That is if they actually look at this field.
		 */
		this.wildcards = OFMatch.OFPFW_ALL 
				& ~OFMatch.OFPFW_NW_DST_ALL 
				& ~OFMatch.OFPFW_NW_DST_ALL;
		this.name = "OVX vFlowTable (incomplete)";
		this.maximumEntries = 100000;
		OVXStatisticsReply reply = new OVXStatisticsReply();
		reply.setXid(msg.getXid());
		reply.setStatisticType(OFStatisticsType.TABLE);
		reply.setStatistics(Collections.singletonList(this));
		reply.setLengthU(OVXStatisticsReply.MINIMUM_LENGTH + this.getLength());
		sw.sendMsg(reply, sw);
	}

	@Override
	public void virtualizeStatistic(final PhysicalSwitch sw,
			final OVXStatisticsReply msg) {
		// TODO Auto-generated method stub

	}

}
