/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

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

    private Logger log = LogManager.getLogger(OVXStatisticsRequest.class.getName());

    @Override
    public void devirtualize(OVXSwitch sw) {
	switch(this.statisticType) {
	    // Desc, vendor, table stats have no body. fuckers.
	    case DESC:
		new OVXDescriptionStatistics().devirtualizeStatistic(sw, this);;
		break;
	    case TABLE:
		new OVXTableStatistics().devirtualizeStatistic(sw, this);
		break;
	    case VENDOR:
		new OVXVendorStatistics().devirtualizeStatistic(sw, this);
		break;
	    default:
		try {
		    OFStatistics stat = this.getStatistics().get(0);
		    ((DevirtualizableStatistic)stat).devirtualizeStatistic(sw, this);
		} catch (ClassCastException e) {
		    log.error("Statistic received is not devirtualizable {}", this);
		}  

	}

    }

}
