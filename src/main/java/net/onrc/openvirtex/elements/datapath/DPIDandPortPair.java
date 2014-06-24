/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

public class DPIDandPortPair {
    private final DPIDandPort src;
    private final DPIDandPort dst;

    public DPIDandPortPair(final DPIDandPort src, final DPIDandPort dst) {
        this.src = src;
        this.dst = dst;
    }

    public DPIDandPort getSrc() {
        return this.src;
    }

    public DPIDandPort getDst() {
        return this.dst;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final DPIDandPortPair other = (DPIDandPortPair) obj;
        if (this.dst == null) {
            if (other.dst != null) {
                return false;
            }
        } else
            if (!this.dst.equals(other.dst)) {
                return false;
            }
        if (this.src == null) {
            if (other.src != null) {
                return false;
            }
        } else
            if (!this.src.equals(other.src)) {
                return false;
            }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.dst == null ? 0 : this.dst.hashCode());
        result = prime * result + (this.src == null ? 0 : this.src.hashCode());
        return result;
    }
}
