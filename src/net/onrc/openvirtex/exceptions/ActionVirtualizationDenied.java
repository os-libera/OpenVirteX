package net.onrc.openvirtex.exceptions;

public class ActionVirtualizationDenied extends Exception {

    private static final long serialVersionUID = 1L;

    public ActionVirtualizationDenied() {
	super();
    }

    public ActionVirtualizationDenied(final String msg) {
	super(msg);
    }

    public ActionVirtualizationDenied(final Throwable msg) {
	super(msg);
    }

}
