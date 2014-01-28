/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

/**
 * Based on XidPair by capveg
 */
public class XidPair<T> {

	int xid;
	T sw;

	public XidPair(final int x, final T sw) {
		this.xid = x;
		this.sw = sw;
	}

	public void setXid(final int x) {
		this.xid = x;
	}

	public int getXid() {
		return this.xid;
	}

	public void setSwitch(final T sw) {
		this.sw = sw;
	}

	public T getSwitch() {
		return this.sw;
	}

}
