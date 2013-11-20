/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.statistics.VirtualizableStatistic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFStatisticsType;

public class OVXStatisticsReply extends OFStatisticsReply implements
		Virtualizable {

	private final Logger log = LogManager.getLogger(OVXStatisticsReply.class
			.getName());

	@Override
	public void virtualize(final PhysicalSwitch sw) {
		/*
		 * The entire stat message will be handled in the 
		 * specific stattype handler. 
		 * 
		 * This means that for stattypes that have a list
		 * of replies the handles will have to call 
		 * getStatistics to handle them all.
		 */
		try {
			
			if (this.getStatistics().size() > 0) {
				VirtualizableStatistic stat = (VirtualizableStatistic) this.getStatistics().get(0);
				stat.virtualizeStatistic(sw, this);
			} else if (this.getStatisticType() == OFStatisticsType.FLOW) {
				sw.setFlowStatistics(null);
			}
		    
		} catch (final ClassCastException e) {
			this.log.error("Statistic received is not virtualizable {}", this);
		}

	}

}
