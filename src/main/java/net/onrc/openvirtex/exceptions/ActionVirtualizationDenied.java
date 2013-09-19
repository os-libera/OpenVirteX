package net.onrc.openvirtex.exceptions;

import org.openflow.protocol.OFError.OFBadActionCode;

public class ActionVirtualizationDenied extends Exception {

	private static final long serialVersionUID = 1L;

	private final OFBadActionCode code;

	public ActionVirtualizationDenied(final String msg,
			final OFBadActionCode code) {
		super(msg);
		this.code = code;
	}

	public OFBadActionCode getErrorCode() {
		return this.code;
	}
}
