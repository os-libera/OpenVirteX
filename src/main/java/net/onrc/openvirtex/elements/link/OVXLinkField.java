/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.link;

/**
 * The Enum OVXLinkField. Used to identify the field(s) used to encapsulate the
 * virtual link information inside the packet. Currently supported values are
 * mac addresses and vlan.
 */
public enum OVXLinkField {

    /** The mac address. */
    MAC_ADDRESS((byte) 0),

    /** The vlan. */
    VLAN((byte) 1);

    /** The value. */
    protected byte value;

    /**
     * Instantiates a new OVX link field.
     * 
     * @param value
     *            the value
     */
    private OVXLinkField(final byte value) {
	this.value = value;
    }

    /**
     * Gets the value.
     * 
     * @return the value
     */
    public byte getValue() {
	return this.value;
    }
}
