/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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
