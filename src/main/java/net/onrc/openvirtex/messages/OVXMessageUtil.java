package net.onrc.openvirtex.messages;



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



}
