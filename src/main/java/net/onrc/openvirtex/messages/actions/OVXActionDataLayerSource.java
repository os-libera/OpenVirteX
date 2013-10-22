/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.actions;

import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerSource;

public class OVXActionDataLayerSource extends OFActionDataLayerSource implements
		VirtualizableAction {

	@Override
	public void virtualize(final OVXSwitch sw,
			final List<OFAction> approvedActions, final OVXMatch match)
			throws ActionVirtualizationDenied {
		final MACAddress mac = MACAddress.valueOf(this.dataLayerAddress);
		try {
			final Integer tid = sw.getMap().getMAC(mac);
			if (tid != sw.getTenantId()) {
				throw new ActionVirtualizationDenied("Target mac " + mac
						+ " is not in virtual network " + sw.getTenantId(),
						OFBadActionCode.OFPBAC_EPERM);
			}
			approvedActions.add(this);
		} catch (AddressMappingException e) {
		    
		}
	}

}
