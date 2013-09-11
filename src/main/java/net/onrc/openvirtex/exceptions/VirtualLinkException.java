package net.onrc.openvirtex.exceptions;

public class VirtualLinkException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public VirtualLinkException() {
	super();
    }

    public VirtualLinkException(String msg) {
	super(msg);
    }

    public VirtualLinkException(Throwable msg) {
	super(msg);
    }
}
