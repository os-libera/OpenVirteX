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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;

public class ManualRoute implements Routable {

    /** The log. */
    private static Logger log = LogManager.getLogger(ManualRoute.class
            .getName());

    @Override
    public LinkedList<PhysicalLink> computePath(final OVXPort srcPort,
            final OVXPort dstPort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SwitchRoute getRoute(final OVXBigSwitch vSwitch,
            final OVXPort srcPort, final OVXPort dstPort) {
        // return route that was set manually
        // TODO : throw 'route not initialized' type of exception if null
        if (vSwitch.getRouteMap().get(srcPort) == null) {
            return null;
        } else {
            return vSwitch.getRouteMap().get(srcPort).get(dstPort);
        }
    }

    @Override
    public String getName() {
        return "manual";
    }

    @Override
    public void setLinkPath(OVXLink ovxLink) {
        log.warn("The manual routing should never call the method [setLinkPath(OVXLink)]. "
                + "To define a manual path for the virtual link, use the API call [SetLinkPath].");

    }

}
