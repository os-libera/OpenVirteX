/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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
