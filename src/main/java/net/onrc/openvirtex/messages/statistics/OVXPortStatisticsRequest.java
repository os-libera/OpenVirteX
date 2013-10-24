/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFStatisticsType;

public class OVXPortStatisticsRequest extends OFPortStatisticsRequest implements
		DevirtualizableStatistic {

	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		List<OVXPortStatisticsReply> replies = new LinkedList<OVXPortStatisticsReply>();
		int length = 0;
		if (this.portNumber == OFPort.OFPP_NONE.getValue()) {
			for (OVXPort p : sw.getPorts().values()) {
				OVXPortStatisticsReply reply =
						p.getPhysicalPort().getParentSwitch().getPortStat(p.getPhysicalPort().getPortNumber());
				if (reply != null) {
					/*
					 * Setting it here will also update the reference
					 * but this should not matter since we index our 
					 * port stats struct by physical port number 
					 * (so this info is not lost) and we always rewrite 
					 * the port num to the virtual port number. 
					 */
					reply.setPortNumber(p.getPortNumber());
					replies.add(reply);
					length += reply.getLength();
				}
			}
			OVXStatisticsReply rep = new OVXStatisticsReply();
			rep.setStatisticType(OFStatisticsType.PORT);
			rep.setStatistics(replies);
			rep.setXid(msg.getXid());
			rep.setLengthU(OVXStatisticsReply.MINIMUM_LENGTH + length);
			sw.sendMsg(rep, sw);
		}
		
	}
		
}
