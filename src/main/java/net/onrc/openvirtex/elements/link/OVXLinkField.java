/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
/**
 * 
 */
package net.onrc.openvirtex.elements.link;

/**
 * @author gerola
 *
 */
public enum OVXLinkField {
	MAC_ADDRESS((byte) 0), 
	VLAN((byte) 1);

	protected byte value;

	private OVXLinkField(byte value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public byte getValue() {
		return value;
	}
}
