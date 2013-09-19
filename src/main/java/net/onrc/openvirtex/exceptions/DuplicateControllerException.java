package net.onrc.openvirtex.exceptions;

public class DuplicateControllerException extends IllegalArgumentException {

	/**
     * 
     */
	private static final long serialVersionUID = 6957434977838246116L;

	public DuplicateControllerException() {
		super();
	}

	public DuplicateControllerException(final String msg) {
		super(msg);
	}

	public DuplicateControllerException(final Throwable msg) {
		super(msg);
	}
}
