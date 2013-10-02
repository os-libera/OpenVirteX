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
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFPortMod;

public class OVXPortMod extends OFPortMod implements Devirtualizable {

	@Override
	public void devirtualize(final OVXSwitch sw) {
		// TODO Auto-generated method stub
		// assume port numbers are virtual
		final OVXPort p = sw.getPort(this.getPortNumber());
		if (p == null) {
			sw.sendMsg(OVXMessageUtil.makeErrorMsg(
					OFBadRequestCode.OFPBRC_EPERM, this), sw);
			return;
		}
		// set physical port number - anything else to do?
		final PhysicalPort phyPort = p.getPhysicalPort();
		this.setPortNumber(phyPort.getPortNumber());

		OVXMessageUtil.translateXid(this, p);
	}

}
