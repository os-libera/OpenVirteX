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
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

public class OVXFlowRemoved extends OFFlowRemoved implements Virtualizable {
	
	Logger log = LogManager.getLogger(OVXFlowRemoved.class.getName());

	@Override
	public void virtualize(final PhysicalSwitch sw) {
		
		long ck = this.cookie;
		int tid = (int) (ck >> 32);
		if (!(sw.getMap().hasVirtualSwitch(sw, tid))) {
			return;
		}
		
		try {
			OVXSwitch vsw = sw.getMap().getVirtualSwitch(sw, tid);
			OVXFlowMod fm = vsw.getFlowMod(this.cookie);
			/* can be null if we are a Big Switch, and receive multiple same-cookie FR's
			 * from multiple PhysicalSwitches */
			if (fm == null) {
				return;
			}
			/* send north ONLY if tenant controller wanted a FlowRemoved for the FlowMod*/
			if (fm.hasFlag(OFFlowMod.OFPFF_SEND_FLOW_REM)) {
				writeFields(fm);
				vsw.sendMsg(this, sw);
			}
			vsw.deleteFlowMod(ck);
		} catch (SwitchMappingException e) {
			log.warn("Received FlowRemoved for nonexisting tenant [{}]", tid);
		}
	}

	/**
	 * rewrites the fields of this message using values from the supplied FlowMod.  
	 * 
	 * @param fm the original FlowMod associated with this FlowRemoved
	 * @return the physical cookie 
	 */
	private void writeFields(OVXFlowMod fm) {
		this.cookie = fm.getCookie();
		this.match = fm.getMatch();
		this.priority = fm.getPriority();
		this.idleTimeout = fm.getIdleTimeout();
	}
	
	@Override
	public String toString() {
		return "OVXFlowRemoved: cookie=" + this.cookie
				+ " priority=" + this.priority
				+ " match=" + this.match
				+ " reason=" + this.reason;
	}

}
