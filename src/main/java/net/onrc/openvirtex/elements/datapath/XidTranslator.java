/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import org.openflow.util.LRULinkedHashMap;

/**
 * based on Flowvisor XidTranslator by capveg
 */
public class XidTranslator<T> {

	static final int MIN_XID = 256;
	static final int INIT_SIZE = 1 << 10;
	static final int MAX_SIZE = 1 << 14; // must be larger than the max lifetime
											// of an XID * rate of
											// mesgs/sec
	int nextID;
	LRULinkedHashMap<Integer, XidPair<T>> xidMap;

	public XidTranslator() {
		this.nextID = XidTranslator.MIN_XID;
		this.xidMap = new LRULinkedHashMap<Integer, XidPair<T>>(
				XidTranslator.INIT_SIZE, XidTranslator.MAX_SIZE);
	}

	/**
	 * Recovers the source of the message transaction by Xid.
	 * 
	 * @param xid
	 * @return
	 */
	public XidPair<T> untranslate(final int xid) {
		return this.xidMap.get(Integer.valueOf(xid));
	}

	/**
	 * @return the new Xid for the message.
	 */
	public int translate(final int xid, final T sw) {
		final int ret = this.nextID++;
		if (this.nextID < XidTranslator.MIN_XID) {
			this.nextID = XidTranslator.MIN_XID;
		}
		this.xidMap.put(Integer.valueOf(ret), new XidPair<T>(xid, sw));
		return ret;
	}

}
