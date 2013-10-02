/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;

public class OVXFlowStatisticsRequest extends OFFlowStatisticsRequest implements
		DevirtualizableStatistic {

	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		// TODO Auto-generated method stub
		final OVXPort p = sw.getPort(this.getMatch().getInputPort());
		if (p == null) {
			sw.sendMsg(OVXMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_EPERM, msg), sw);
			return;
		}
		OVXMessageUtil.translateXid(msg, p);
	}

}
