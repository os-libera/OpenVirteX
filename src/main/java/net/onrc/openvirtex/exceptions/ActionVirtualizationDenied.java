/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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
