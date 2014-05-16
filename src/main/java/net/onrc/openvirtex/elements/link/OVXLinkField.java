/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.link;

/**
 * Enum used to identify the field(s) used to encapsulate the virtual link
 * information inside the packet. Currently supported values are MAC addresses
 * and VLAN.
 */
public enum OVXLinkField {

    /**
     * MAC address field.
     */
    MAC_ADDRESS((byte) 0),
    /**
     * VLAN field.
     */
    VLAN((byte) 1);
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
