package net.onrc.openvirtex.messages;



import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
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

    /**
     * Xid translation based on port for "accurate" translation with a specific PhysicalSwitch. 
     * @param msg
     * @param inPort
     * @return
     */
    public static OVXSwitch translateXid(OFMessage msg, OVXPort inPort) {
        OVXSwitch vsw = inPort.getParentSwitch();
        int xid = vsw.translate(msg, inPort);
        msg.setXid(xid);
        return vsw;
    }

    /**
     * Xid translation based on OVXSwitch, for cases where port is indeterminable 
     * 
     * @param msg
     * @param vsw
     * @return new Xid for msg 
     */
    public static Integer translateXid(OFMessage msg, OVXSwitch vsw) {
	//this returns the original XID for a BigSwitch
	Integer xid = vsw.translate(msg, null);
        msg.setXid(xid);
        return xid;
    }

    /**
     * translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * for when port is known.  
     * @param msg
     * @param inPort
     */
    public static void translateXidAndSend(OFMessage msg, OVXPort inPort) {
        OVXSwitch vsw = OVXMessageUtil.translateXid(msg, inPort);
        vsw.sendSouth(msg);
    }

    /**
     * translates the Xid of a PhysicalSwitch-bound message and sends it there.
     * for when port is not known.  
     * @param msg
     * @param inPort
     */
    public static void translateXidAndSend(OFMessage msg, OVXSwitch vsw) {
	int newXid = OVXMessageUtil.translateXid(msg, vsw);
	
	if (vsw instanceof OVXBigSwitch) {
	    //no port info for BigSwitch, to all its PhysicalSwitches. Is this ok?
	    for (PhysicalSwitch psw : vsw.getMap().getPhysicalSwitches(vsw)) {
		int xid = psw.translate(msg, vsw);
		msg.setXid(xid);
		psw.sendMsg(msg, vsw);
		msg.setXid(newXid);
	    }
	} else {
	    vsw.sendSouth(msg);
	}
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
            //log error 
            return;
        }
        vsw.sendMsg(msg, psw);
    }

}
