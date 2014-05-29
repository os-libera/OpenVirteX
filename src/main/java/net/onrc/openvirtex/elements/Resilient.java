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
package net.onrc.openvirtex.elements;

/**
 * The interface that a Component must implement if capable of recovering from
 * failure. A Resilient class is always a Component.
 */
public interface Resilient {
    /**
     * Tries to recover from the failure of Component c. Recovery is defined as
     * continued function despite failure of underlying infrastructure or
     * subcomponents.
     *
     * @param c
     *            Component that had failed
     * @return true if successful.
     */
    public boolean tryRecovery(Component c);

    /**
     * Actions taken when c returns to a functional state, in order to return it
     * to the state prior to failure.
     *
     * @param c
     *            Component returning from failed state
     * @return true if successful.
     */
    public boolean tryRevert(Component c);
}