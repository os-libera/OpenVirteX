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
package net.onrc.openvirtex.routing;

import java.util.LinkedList;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.PortMappingException;

public interface Routable {

    public LinkedList<PhysicalLink> computePath(OVXPort srcPort, OVXPort dstPort);

    /**
     * @param vSwitch
     *            The virtual big switch
     * @param srcPort
     *            The ingress port on the big switch
     * @param dstPort
     *            The egress port on the big switch
     * @return the switch route
     */
    public SwitchRoute getRoute(OVXBigSwitch vSwitch, OVXPort srcPort,
            OVXPort dstPort);

    /**
     * Gets the name of the routing policy.
     *
     * @return the name
     */
    public String getName();

    /**
     * Sets end-points to a route.
     *
     * @param ovxLink
     *            The link representing the end-points of a route
     * @throws PortMappingException
     */
    public void setLinkPath(OVXLink ovxLink) throws PortMappingException;
}
