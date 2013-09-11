package net.onrc.openvirtex.exceptions;

public class InvalidDPIDException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidDPIDException() {
	super();
    }

    public InvalidDPIDException(String msg) {
	super(msg);
    }

    public InvalidDPIDException(Throwable msg) {
	super(msg);
    }
}
