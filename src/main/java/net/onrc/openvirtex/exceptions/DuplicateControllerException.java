package net.onrc.openvirtex.exceptions;

public class DuplicateControllerException extends IllegalArgumentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6957434977838246116L;

    public DuplicateControllerException() {
	super();
    }

    public DuplicateControllerException(String msg) {
	super(msg);
    }

    public DuplicateControllerException(Throwable msg) {
	super(msg);
    }
}
