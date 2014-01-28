/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;




import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openflow.protocol.OFMessage;

public class OVXSingleSwitch extends OVXSwitch {
	
	
	

	private static Logger log = LogManager.getLogger(OVXSingleSwitch.class
			.getName());

	public OVXSingleSwitch(final long switchId, final int tenantId, boolean isRoled) {
		super(switchId, tenantId, isRoled);
	}

	@Override
	public boolean removePort(final Short portNumber) {
		if (!this.portMap.containsKey(portNumber)) {
			return false;
		} else {
			// TODO: this should generate a portstatus message to the ctrl
			this.portMap.remove(portNumber);
			return true;
		}
	}

	

	@Override
	// TODO: this is probably not optimal
	public void sendSouth(final OFMessage msg, final OVXPort inPort) {
		PhysicalSwitch psw = getPhySwitch(inPort);
		log.debug("Sending packet to sw {}: {}", psw.getName(), msg);
		psw.sendMsg(msg, this);
	}

	@Override
	public int translate(final OFMessage ofm, final OVXPort inPort) {
		// get new xid from only PhysicalSwitch tied to this switch
		PhysicalSwitch psw = getPhySwitch(inPort);
		return psw.translate(ofm, this);
	}
	
	private PhysicalSwitch getPhySwitch(OVXPort inPort) {
	    	PhysicalSwitch psw = null;
		if (inPort == null) {
			try {
				psw = this.map.getPhysicalSwitches(this).get(0);
			} catch (SwitchMappingException e) {
				log.warn("Cannot recover physical switch : {}", e);
			}
		} else {
			return inPort.getPhysicalPort().getParentSwitch();
		}
		return psw;
	}
}
