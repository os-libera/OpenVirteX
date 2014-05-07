/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.XidPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFMessage;

/**
 * Utility class for OVX messages. Implements methods
 * for creating error messages and transaction ID
 * mappings.
 */
public final class OVXMessageUtil {

    private static Logger log = LogManager.getLogger(OVXMessageUtil.class.getName());

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private OVXMessageUtil() {
    }

    /**
     * Makes an OpenFlow error message for a bad action and
     * given OpenFlow message.
     *
     * @param code the bad action code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeError(final OFBadActionCode code,
            final OFMessage msg) {
        final OVXError err = new OVXError();
        err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed flow mod and
     * given OpenFlow message.
     *
     * @param code the failed flow mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFFlowModFailedCode code,
            final OFMessage msg) {
        final OVXError err = new OVXError();
        err.setErrorType(OFErrorType.OFPET_FLOW_MOD_FAILED);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed port mod and
     * given OpenFlow message.
     *
     * @param code the failed port mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFPortModFailedCode code,
            final OFMessage msg) {
        final OVXError err = new OVXError();
        err.setErrorType(OFErrorType.OFPET_PORT_MOD_FAILED);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a bad request and
     * given OpenFlow message.
     *
     * @param code the bad request code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFBadRequestCode code,
            final OFMessage msg) {
        final OVXError err = new OVXError();
        err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Xid translation based on port for "accurate" translation with a specific
     * PhysicalSwitch.
     *
     * @param msg the OpenFlow message
     * @param inPort the virtual input port instance
     * @return the virtual switch
     */
    public static OVXSwitch translateXid(final OFMessage msg,
            final OVXPort inPort) {
        final OVXSwitch vsw = inPort.getParentSwitch();
        final int xid = vsw.translate(msg, inPort);
        msg.setXid(xid);
        return vsw;
    }

    /**
     * Xid translation based on OVXSwitch, for cases where port cannot be
     * determined.
     *
     * @param msg the OpenFlow message
     * @param vsw the virtual switch instance
     * @return new Xid for msg
     */
    public static Integer translateXid(final OFMessage msg, final OVXSwitch vsw) {
        // this returns the original XID for a BigSwitch
        final Integer xid = vsw.translate(msg, null);
        msg.setXid(xid);
        return xid;
    }

    /**
     * Translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * For when port is known.
     *
     * @param msg the OpenFlow message
     * @param inPort the virtual input port instance
     */
    public static void translateXidAndSend(final OFMessage msg,
            final OVXPort inPort) {
        final OVXSwitch vsw = OVXMessageUtil.translateXid(msg, inPort);
        vsw.sendSouth(msg, inPort);
    }

    /**
     * Translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * For when port is not known.
     *
     * @param msg the OpenFlow message
     * @param vsw the virtual switch instance
     */
    public static void translateXidAndSend(final OFMessage msg,
            final OVXSwitch vsw) {
        final int newXid = OVXMessageUtil.translateXid(msg, vsw);

        if (vsw instanceof OVXBigSwitch) {
            // no port info for BigSwitch, to all its PhysicalSwitches. Is this
            // ok?
            try {
                for (final PhysicalSwitch psw : vsw.getMap()
                        .getPhysicalSwitches(vsw)) {
                    final int xid = psw.translate(msg, vsw);
                    msg.setXid(xid);
                    psw.sendMsg(msg, vsw);
                    msg.setXid(newXid);
                }
            } catch (SwitchMappingException e) {
                log.error("Switch {} is not mapped to any physical switches.", vsw);
            }
        } else {
            vsw.sendSouth(msg, null);
        }
    }

    /**
     * Undoes the XID translation and returns the original virtual switch.
     *
     * @param msg the OpenFlow message
     * @param psw the physical switch
     * @return the virtual switch
     */
    public static OVXSwitch untranslateXid(final OFMessage msg,
            final PhysicalSwitch psw) {
        final XidPair<OVXSwitch> pair = psw.untranslate(msg);
        if (pair == null) {
            return null;
        }
        msg.setXid(pair.getXid());
        return pair.getSwitch();
    }

    /**
     * Undoes the Xid translation and tries to send the resulting message to the
     * origin OVXSwitch.
     *
     * @param msg the OpenFlow message
     * @param psw the physical switch
     */
    public static void untranslateXidAndSend(final OFMessage msg,
            final PhysicalSwitch psw) {
        final OVXSwitch vsw = OVXMessageUtil.untranslateXid(msg, psw);
        if (vsw == null) {
            log.error("Cound not untranslate XID for switch {}", psw);
            return;
        }
        vsw.sendMsg(msg, psw);
    }

}
