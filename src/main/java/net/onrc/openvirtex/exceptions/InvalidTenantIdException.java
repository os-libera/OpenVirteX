package net.onrc.openvirtex.exceptions;

/**
 * Admin tries to create a new virtual switches in a virtual network but the
 * tenantId that has been provided does not correspond to any tenantId that has
 * been created.
 */
public class InvalidTenantIdException extends IllegalArgumentException {

	private static final long serialVersionUID = 6957434977838246116L;

	public InvalidTenantIdException() {
		super();
	}

	public InvalidTenantIdException(final String msg) {
		super(msg);
	}

	public InvalidTenantIdException(final Throwable msg) {
		super(msg);
	}
}
