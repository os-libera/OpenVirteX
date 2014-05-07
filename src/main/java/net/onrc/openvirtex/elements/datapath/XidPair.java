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
package net.onrc.openvirtex.elements.datapath;

/**
 * Stores a switch and a transaction ID.
 * Based on XidPair by capveg.
 *
 * @param <T> generic switch type
 */
public class XidPair<T> {

    private int xid;
    private T sw;

    /**
     * Creates an instance for a given XID and switch.
     *
     * @param x the XID
     * @param sw the switch
     */
    public XidPair(final int x, final T sw) {
        this.xid = x;
        this.sw = sw;
    }

    /**
     * Sets the transaction ID.
     *
     * @param x the XID
     */
    public void setXid(final int x) {
        this.xid = x;
    }

    /**
     * Gets the transaction ID.
     *
     * @return the XID
     */
    public int getXid() {
        return this.xid;
    }

    /**
     * Sets the switch.
     *
     * @param sw the switch
     */
    public void setSwitch(final T sw) {
        this.sw = sw;
    }

    /**
     * Gets the switch.
     *
     * @return the switch
     */
    public T getSwitch() {
        return this.sw;
    }

}
