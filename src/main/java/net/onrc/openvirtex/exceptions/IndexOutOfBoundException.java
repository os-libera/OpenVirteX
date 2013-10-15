/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.exceptions;

public class IndexOutOfBoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1153095359673027769L;
    
    public IndexOutOfBoundException() {
	super();
    }

    public IndexOutOfBoundException(final String msg) {
	super(msg);
    }

    public IndexOutOfBoundException(final Throwable msg) {
	super(msg);
    }
    
}
