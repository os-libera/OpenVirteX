package net.onrc.openvirtex.exceptions;

public class InvalidPortException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidPortException() {
	super();
    }

    public InvalidPortException(String msg) {
	super(msg);
    }

    public InvalidPortException(Throwable msg) {
	super(msg);
    }
}
