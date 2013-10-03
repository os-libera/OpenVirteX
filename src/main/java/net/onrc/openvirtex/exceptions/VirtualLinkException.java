/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.exceptions;

/**
 * This exception is thrown when the admin tries to create the same virtual link
 * multiple times. Each virtual Link can only be create once per virtual network
 */
public class VirtualLinkException extends IllegalArgumentException {

	private static final long serialVersionUID = 6957434977838246116L;

	public VirtualLinkException() {
		super();
	}

	public VirtualLinkException(final String msg) {
		super(msg);
	}

	public VirtualLinkException(final Throwable msg) {
		super(msg);
	}
}
