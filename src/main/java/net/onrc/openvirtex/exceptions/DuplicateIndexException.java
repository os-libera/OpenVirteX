package net.onrc.openvirtex.exceptions;

public class DuplicateIndexException extends Exception {

	private static final long serialVersionUID = 3725666959913773107L;

	public DuplicateIndexException() {
		super();
	}

	public DuplicateIndexException(final String msg) {
		super(msg);
	}

	public DuplicateIndexException(final Throwable msg) {
		super(msg);
	}
}
