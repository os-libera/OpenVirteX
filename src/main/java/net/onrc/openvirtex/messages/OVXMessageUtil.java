/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.XidPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFMessage;

public class OVXMessageUtil {

	public static OFMessage makeError(final OFBadActionCode code,
			final OFMessage msg) {
		final OVXError err = new OVXError();
		err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		err.setXid(msg.getXid());
		return err;
	}

	public static OFMessage makeErrorMsg(final OFFlowModFailedCode code,
			final OFMessage msg) {
		final OVXError err = new OVXError();
		err.setErrorType(OFErrorType.OFPET_FLOW_MOD_FAILED);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		err.setXid(msg.getXid());
		return err;
	}

	public static OFMessage makeErrorMsg(final OFPortModFailedCode code,
			final OFMessage msg) {
		final OVXError err = new OVXError();
		err.setErrorType(OFErrorType.OFPET_PORT_MOD_FAILED);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);
		err.setXid(msg.getXid());
		return err;
	}

	public static OFMessage makeErrorMsg(final OFBadRequestCode code,
			final OFMessage msg) {
		final OVXError err = new OVXError();
		err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
		err.setErrorCode(code);
		err.setOffendingMsg(msg);

		return err;
	}

	/**
	 * Xid translation based on port for "accurate" translation with a specific
	 * PhysicalSwitch.
	 * 
	 * @param msg
	 * @param inPort
	 * @return
	 */
	public static OVXSwitch translateXid(final OFMessage msg,
			final OVXPort inPort) {
		final OVXSwitch vsw = inPort.getParentSwitch();
		final int xid = vsw.translate(msg, inPort);
		msg.setXid(xid);
		return vsw;
	}

	/**
	 * Xid translation based on OVXSwitch, for cases where port is
	 * indeterminable
	 * 
	 * @param msg
	 * @param vsw
	 * @return new Xid for msg
	 */
	public static Integer translateXid(final OFMessage msg, final OVXSwitch vsw) {
		// this returns the original XID for a BigSwitch
		final Integer xid = vsw.translate(msg, null);
		msg.setXid(xid);
		return xid;
	}

	/**
	 * translates the Xid of a PhysicalSwitch-bound message and sends it there.
	 * for when port is known.
	 * 
	 * @param msg
	 * @param inPort
	 */
	public static void translateXidAndSend(final OFMessage msg,
			final OVXPort inPort) {
		final OVXSwitch vsw = OVXMessageUtil.translateXid(msg, inPort);
		vsw.sendSouth(msg, inPort);
	}

	/**
	 * translates the Xid of a PhysicalSwitch-bound message and sends it there.
	 * for when port is not known.
	 * 
	 * @param msg
	 * @param inPort
	 */
	public static void translateXidAndSend(final OFMessage msg,
			final OVXSwitch vsw) {
		final int newXid = OVXMessageUtil.translateXid(msg, vsw);

		if (vsw instanceof OVXBigSwitch) {
			// no port info for BigSwitch, to all its PhysicalSwitches. Is this ok?
			try {
				for (final PhysicalSwitch psw : vsw.getMap().getPhysicalSwitches(vsw)) {
					final int xid = psw.translate(msg, vsw);
					msg.setXid(xid);
					psw.sendMsg(msg, vsw);
					msg.setXid(newXid);
				}
                        } catch (SwitchMappingException e) {
				//log warning                     
                        }
		} else {
			vsw.sendSouth(msg, null);
		}
	}

	public static OVXSwitch untranslateXid(final OFMessage msg,
			final PhysicalSwitch psw) {
		final XidPair pair = psw.untranslate(msg);
		if (pair == null) {
			return null;
		}
		msg.setXid(pair.getXid());
		return pair.getSwitch();
	}

	/**
	 * undoes the Xid translation and tries to send the resulting message to the
	 * origin OVXSwitch.
	 * 
	 * @param msg
	 * @param psw
	 */
	public static void untranslateXidAndSend(final OFMessage msg,
			final PhysicalSwitch psw) {
		final OVXSwitch vsw = OVXMessageUtil.untranslateXid(msg, psw);
		if (vsw == null) {
			// log error
			return;
		}
		vsw.sendMsg(msg, psw);
	}

}
