package net.onrc.openvirtex.exceptions;

import org.openflow.protocol.OFError.OFBadActionCode;

public class ActionVirtualizationDenied extends Exception {

    
    private static final long serialVersionUID = 1L;

    private OFBadActionCode code;
    
    public ActionVirtualizationDenied(String msg, OFBadActionCode code) {
	super(msg);
	this.code = code;
    }

    public OFBadActionCode getErrorCode() {
	return this.code;
    }
}
