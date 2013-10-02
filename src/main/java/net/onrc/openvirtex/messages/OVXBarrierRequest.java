/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;

import org.openflow.protocol.OFBarrierRequest;

public class OVXBarrierRequest extends OFBarrierRequest implements
		Devirtualizable {

	@Override
	public void devirtualize(final OVXSwitch sw) {
		// TODO Auto-generated method stub

	}

}
