/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.actions;

import java.util.List;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.protocol.OVXMatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;

public class OVXActionNetworkLayerDestination extends
		OFActionNetworkLayerDestination implements VirtualizableAction {

	private final Logger log = LogManager
			.getLogger(OVXActionNetworkLayerDestination.class.getName());

	@Override
	public void virtualize(final OVXSwitch sw,
			final List<OFAction> approvedActions, final OVXMatch match)
			throws ActionVirtualizationDenied {
		final Mappable map = sw.getMap();
		final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
				this.networkAddress);
		PhysicalIPAddress pip = map.getPhysicalIP(vip, sw.getTenantId());
		if (pip == null) {
			pip = new PhysicalIPAddress(map.getVirtualNetwork(sw.getTenantId())
					.nextIP());
			this.log.debug(
					"Adding IP mapping {} -> {} for tenant {} at switch {}",
					vip, pip, sw.getTenantId(), sw.getName());
			map.addIP(pip, vip);
		}
		this.networkAddress = pip.getIp();
		approvedActions.add(this);
	}

}
