/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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

package net.onrc.openvirtex.messages.statistics;

import java.util.Collections;

import net.onrc.openvirtex.core.OpenVirteX;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

public class OVXDescriptionStatistics extends OFDescriptionStatistics implements
		VirtualizableStatistic, DevirtualizableStatistic {

	/**
	 * Received a Description stats request from the controller Create a reply
	 * object populated with the virtual switch params and send it back to the
	 * controller.
	 */
	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		final OVXStatisticsReply reply = new OVXStatisticsReply();

		final OVXDescriptionStatistics desc = new OVXDescriptionStatistics();

		desc.setDatapathDescription(OVXSwitch.DPDESCSTRING);
		desc.setHardwareDescription("virtual hardware");
		desc.setManufacturerDescription("Open Networking Lab");
		desc.setSerialNumber(sw.getSwitchName());
		desc.setSoftwareDescription(OpenVirteX.VERSION);

		reply.setXid(msg.getXid());
		reply.setLengthU(reply.getLength() + desc.getLength());
		reply.setStatisticType(OFStatisticsType.DESC);
		reply.setStatistics(Collections.singletonList(desc));
		sw.sendMsg(reply, sw);

	}

	@Override
	public void virtualizeStatistic(final PhysicalSwitch sw,
			final OVXStatisticsReply msg) {
		// log.error("Received illegal message form physical network; {}", msg);

	}

}
