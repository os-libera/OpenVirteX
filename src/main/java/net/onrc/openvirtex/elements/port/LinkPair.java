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
package net.onrc.openvirtex.elements.port;

/**
 * Class representing a link connected to a port. Made of two links - one with
 * this port as source, and the other with this port as destination.
 *
 * @param T
 *            generic Link
 */
public class LinkPair<T> {

    // Link with port as destination
    protected T ingressLink;
    // Link with port as source
    protected T egressLink;

    public LinkPair() {
        this.ingressLink = null;
        this.egressLink = null;
    }

    public void setInLink(T link) {
        this.ingressLink = link;
    }

    public void setOutLink(T link) {
        this.egressLink = link;
    }

    /**
     * Remove inbound link (sets it to null).
     */
    public void removeInLink() {
        // should set opposing link accordingly
        this.ingressLink = null;
    }

    /**
     * Removes outbound link (sets it to null).
     */
    public void removeOutLink() {
        // should set opposing link accordingly
        this.egressLink = null;
    }

    /**
     * @return the link with this port as destination
     */
    public T getInLink() {
        return this.ingressLink;
    }

    /**
     * @return the link with this port as source
     */
    public T getOutLink() {
        return this.egressLink;
    }

    /**
     * @return false if both ingress and egress links are null
     */
    public boolean exists() {
        return ((this.ingressLink != null) && (this.egressLink != null));
    }

    @Override
    public String toString() {
        return "LinkPair[" + this.ingressLink.toString() + ":"
                + this.egressLink.toString() + "]";
    }
}
