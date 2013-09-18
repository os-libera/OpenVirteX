package net.onrc.openvirtex.exceptions;

/**
 * This exception is thrown when the admin tries to create the same virtual link multiple times. Each virtual Link can only be create once per virtual network
 */
public class VirtualLinkException extends IllegalArgumentException {

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
