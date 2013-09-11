package net.onrc.openvirtex.exceptions;

public class InvalidTenantIdException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidTenantIdException() {
	super();
    }

    public InvalidTenantIdException(String msg) {
	super(msg);
    }

    public InvalidTenantIdException(Throwable msg) {
	super(msg);
    }
}
