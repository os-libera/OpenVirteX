/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.statistics.DevirtualizableStatistic;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;
import net.onrc.openvirtex.messages.statistics.OVXTableStatistics;
import net.onrc.openvirtex.messages.statistics.OVXVendorStatistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXStatisticsRequest extends OFStatisticsRequest implements
		Devirtualizable {

	private final Logger log = LogManager.getLogger(OVXStatisticsRequest.class
			.getName());

	@Override
	public void devirtualize(final OVXSwitch sw) {
		switch (this.statisticType) {
		// Desc, vendor, table stats have no body. fuckers.
		case DESC:
			new OVXDescriptionStatistics().devirtualizeStatistic(sw, this);
			;
			break;
		case TABLE:
			new OVXTableStatistics().devirtualizeStatistic(sw, this);
			break;
		case VENDOR:
			new OVXVendorStatistics().devirtualizeStatistic(sw, this);
			break;
		default:
			try {
				final OFStatistics stat = this.getStatistics().get(0);
				((DevirtualizableStatistic) stat).devirtualizeStatistic(sw,
						this);
			} catch (final ClassCastException e) {
				this.log.error("Statistic received is not devirtualizable {}",
						this);
			}

		}

	}

}
