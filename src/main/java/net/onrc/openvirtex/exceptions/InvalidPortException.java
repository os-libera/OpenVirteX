package net.onrc.openvirtex.exceptions;

/**
 * The port number specified by the client is not valid and available in the
 * physical switch plane.
 */
public class InvalidPortException extends IllegalArgumentException {

	private static final long serialVersionUID = 6957434977838246116L;

	public InvalidPortException() {
		super();
	}

	public InvalidPortException(final String msg) {
		super(msg);
	}

	public InvalidPortException(final Throwable msg) {
		super(msg);
	}
}
