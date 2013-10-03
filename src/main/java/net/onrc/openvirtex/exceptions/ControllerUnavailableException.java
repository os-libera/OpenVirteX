/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.exceptions;

/**
 * Each virtual network should talk to a different controller. If the admin
 * tries to make to virtual networks talk to the same controller then this
 * exception will be thrown.
 */
public class ControllerUnavailableException extends Exception {

	public ControllerUnavailableException(final String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
