/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openflow.protocol.statistics.OFQueueStatisticsRequest;

public class OVXQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements DevirtualizableStatistic {

	Logger log = LogManager.getLogger(OVXQueueStatisticsRequest.class.getName());
	
	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		//TODO
		log.info("Queue statistics handling not yet implemented");
	}

}
