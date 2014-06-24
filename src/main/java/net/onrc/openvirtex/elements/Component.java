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
package net.onrc.openvirtex.elements;

/**
 * Methods implemented by classes that represent network elements. Each
 * component is assumed to implement a state machine with the following states:
 * <ul>
 * <li>INIT - just initialized, not accessible or usable</li>
 * <li>ACTIVE - capable of handling events and accumulating state (flow entries,
 * counters), etc.</li>
 * <li>INACTIVE - halted, not capable of handling events or accumulating state
 * (e.g. a downed interface)</li>
 * <li>STOPPED - removed from network representation (e.g. a failed switch)</li>
 * </ul>
 * Some Components can influence the state of other Components (subcomponents)
 * with their own. In general, a subcomponent's state won't affect the
 * component, but the reverse is not true e.g. ports are removed if a switch is
 * removed, but a switch port can be removed without affecting the whole switch.
 */
public interface Component {

    /**
     * Adds this component to mappings, storage, and makes it accessible to OVX
     * as subscribers to components that this one depends on (sets state to
     * INACTIVE from INIT)
     */
    public void register();

    /**
     * Initializes component's services and sets its state up to be activated.
     *
     * @return true if successfully initialized (sets state to ACTIVE)
     */
    public boolean boot();

    /**
     * Removes this component and its subcomponents from global mapping and
     * unsubsribes this component from others. (sets state to STOPPED). If this
     * component is a Physical entity, it will also attempt to unregister()
     * Virtual components mapped to it.
     */
    public void unregister();

    /**
     * Halts event processing at this component (sets state to INACTIVE) and its
     * subcomponents. If this component is a Physical entity, it will also
     * attempt to tearDown() Virtual components mapped to it.
     *
     * @return true if component is deactivated
     */
    public boolean tearDown();
}