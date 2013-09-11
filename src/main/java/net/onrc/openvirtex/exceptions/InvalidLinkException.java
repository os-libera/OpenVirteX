package net.onrc.openvirtex.exceptions;

public class InvalidLinkException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidLinkException() {
	super();
    }

    public InvalidLinkException(String msg) {
	super(msg);
    }

    public InvalidLinkException(Throwable msg) {
	super(msg);
    }
}
