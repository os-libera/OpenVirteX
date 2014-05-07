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

import org.openflow.util.LRULinkedHashMap;

/**
 * Based on Flowvisor XidTranslator by capveg.
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
