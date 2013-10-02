/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import java.util.List;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.statistics.VirtualizableStatistic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXStatisticsReply extends OFStatisticsReply implements
		Virtualizable {

	private final Logger log = LogManager.getLogger(OVXStatisticsReply.class
			.getName());

	@Override
	public void virtualize(final PhysicalSwitch sw) {
		try {
		    List<? extends OFStatistics> stats = this.getStatistics();
		    for (OFStatistics stat : stats) {
			((VirtualizableStatistic) stat).virtualizeStatistic(sw, this);
		    }
			
		} catch (final ClassCastException e) {
			this.log.error("Statistic received is not virtualizable {}", this);
		}

	}

}
