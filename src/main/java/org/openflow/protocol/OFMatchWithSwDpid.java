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
/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.openflow.protocol;

import org.openflow.util.HexString;

public class OFMatchWithSwDpid {
    protected OFMatch ofMatch;
    protected long switchDataPathId;

    public OFMatchWithSwDpid() {
        this.ofMatch = new OFMatch();
        this.switchDataPathId = 0;
    }

    public OFMatchWithSwDpid(final OFMatch ofm, final long swDpid) {
        this.ofMatch = ofm.clone();
        this.switchDataPathId = swDpid;
    }

    public OFMatch getOfMatch() {
        return this.ofMatch;
    }

    public void setOfMatch(final OFMatch ofMatch) {
        this.ofMatch = ofMatch.clone();
    }

    public long getSwitchDataPathId() {
        return this.switchDataPathId;
    }

    public OFMatchWithSwDpid setSwitchDataPathId(final long dpid) {
        this.switchDataPathId = dpid;
        return this;
    }

    @Override
    public String toString() {
        return "OFMatchWithSwDpid ["
                + HexString.toHexString(this.switchDataPathId) + " "
                + this.ofMatch + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (this.ofMatch == null ? 0 : this.ofMatch.hashCode());
        result = prime * result
                + (int) (this.switchDataPathId ^ this.switchDataPathId >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFMatchWithSwDpid)) {
            return false;
        }
        final OFMatchWithSwDpid other = (OFMatchWithSwDpid) obj;
        if (this.ofMatch == null) {
            if (other.ofMatch != null) {
                return false;
            }
        } else if (!this.ofMatch.equals(other.ofMatch)) {
            return false;
        }
        if (this.switchDataPathId != other.switchDataPathId) {
            return false;
        }
        return true;
    }
}
