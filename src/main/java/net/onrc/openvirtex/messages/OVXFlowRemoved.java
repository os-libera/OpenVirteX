/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMatch;

public class OVXFlowRemoved extends OFFlowRemoved implements Virtualizable {

	@Override
	public void virtualize(final PhysicalSwitch sw) {
		// in response to FlowMod?
		final OVXSwitch vsw = OVXMessageUtil.untranslateXid(this, sw);
		if (vsw == null) {
			// no-body asked for this
			return;
		}

		// TODO need to re-write OFMatch
		final OFMatch ofm = this.getMatch();

		// vsw.sendMsg(this, sw);

	}

}
