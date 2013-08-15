package net.onrc.openvirtex.exceptions;

public class ActionVirtualizationDenied extends Exception {

    
    private static final long serialVersionUID = 1L;

    public ActionVirtualizationDenied() {
	super();
    }

    public ActionVirtualizationDenied(String msg) {
	super(msg);
    }

    public ActionVirtualizationDenied(Throwable msg) {
	super(msg);
    }
    
}
