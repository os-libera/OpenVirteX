package net.onrc.openvirtex.exceptions;

public class ControllerStateException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public ControllerStateException() {
	super();
    }

    public ControllerStateException(String msg) {
	super(msg);
    }

    public ControllerStateException(Throwable msg) {
	super(msg);
    }
}
