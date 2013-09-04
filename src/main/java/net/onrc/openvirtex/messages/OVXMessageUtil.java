package net.onrc.openvirtex.messages;



import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.XidPair;
import net.onrc.openvirtex.elements.port.OVXPort;

import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFMessage;


public class OVXMessageUtil {

    public static OFMessage  makeError(OFBadActionCode code, OFMessage msg) {
	OVXError err = new OVXError();
	err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
	err.setErrorCode(code);
	err.setOffendingMsg(msg);
	err.setXid(msg.getXid());
	return err;
    }

    public static OFMessage makeErrorMsg(OFFlowModFailedCode code, OFMessage msg) {
	OVXError err = new OVXError();
	err.setErrorType(OFErrorType.OFPET_FLOW_MOD_FAILED);
	err.setErrorCode(code);
	err.setOffendingMsg(msg);
	err.setXid(msg.getXid());
	return err;
    }

    public static OFMessage makeErrorMsg(OFPortModFailedCode code, OFMessage msg) {
	OVXError err = new OVXError();
	err.setErrorType(OFErrorType.OFPET_PORT_MOD_FAILED);
	err.setErrorCode(code);
	err.setOffendingMsg(msg);
	err.setXid(msg.getXid());
	return err;
    }

    public static OFMessage makeErrorMsg(OFBadRequestCode code, OFMessage msg) {
	OVXError err = new OVXError();	err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
	err.setErrorCode(code);
	err.setOffendingMsg(msg);
	
	return err;
    }

    public static OVXSwitch translateXid(OFMessage msg, OVXPort inPort) {
        OVXSwitch vsw = inPort.getParentSwitch();
        int xid = vsw.translate(msg, inPort);
        msg.setXid(xid);
        return vsw;
    }

    public static Integer translateXid(OFMessage msg, OVXSwitch vsw) {
        Integer xid = vsw.translate(msg, null);
        msg.setXid(xid);
        return xid;
    }

    /**
     * translates the Xid of a PhysicalSwitch-bound message and sends it there.  
     * @param msg
     * @param inPort
     */
    public static void translateXidAndSend(OFMessage msg, OVXPort inPort) {
        OVXSwitch vsw = OVXMessageUtil.translateXid(msg, inPort);
        vsw.sendSouth(msg);
    }

    public static OVXSwitch untranslateXid(OFMessage msg, PhysicalSwitch psw) {
        XidPair pair = psw.untranslate(msg);
        if (pair == null) {
            return null;
        }
        msg.setXid(pair.getXid());
        return (OVXSwitch) pair.getSwitch();
    }

    /**
     * undoes the Xid translation and tries to send the resulting message to 
     * the origin OVXSwitch.
     * 
     * @param msg
     * @param psw
     */
    public static void untranslateXidAndSend(OFMessage msg, PhysicalSwitch psw) {
        OVXSwitch vsw = OVXMessageUtil.untranslateXid(msg, psw);
        if (vsw == null) {
            return;
        }
        vsw.sendMsg(msg, psw);
    }

}
