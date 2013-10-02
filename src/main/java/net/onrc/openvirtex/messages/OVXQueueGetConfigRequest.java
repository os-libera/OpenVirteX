/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;

import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFQueueGetConfigRequest;

public class OVXQueueGetConfigRequest extends OFQueueGetConfigRequest implements
		Devirtualizable {

	@Override
	public void devirtualize(final OVXSwitch sw) {
		final OVXPort p = sw.getPort(this.getPortNumber());
		if (p == null) {
			sw.sendMsg(OVXMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_EPERM, this), sw);
			return;
		}

		OVXMessageUtil.translateXid(this, p);
	}

}
