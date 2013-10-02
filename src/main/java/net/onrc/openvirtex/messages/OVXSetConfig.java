/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFSetConfig;

public class OVXSetConfig extends OFSetConfig implements Devirtualizable {

	private final Logger log = LogManager.getLogger(OVXSetConfig.class
			.getName());

	@Override
	public void devirtualize(final OVXSwitch sw) {

		sw.setMissSendLen(this.missSendLength);
		this.log.info("Setting miss send length to {} for OVXSwitch {}",
				this.missSendLength, sw.getSwitchId());

		OVXMessageUtil.translateXid(this, sw);
		// don't send since we always want full pkt?
	}

}
