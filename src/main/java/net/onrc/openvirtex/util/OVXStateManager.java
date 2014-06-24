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
package net.onrc.openvirtex.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.Component;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Resilient;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class that coordinates the states between PhysicalPorts/Links and
 * OVXPorts/Links and SwitchRoutes mapped to them.
 */
public class OVXStateManager {

    protected static Logger log = LogManager.getLogger(OVXStateManager.class
            .getName());
    protected static Mappable map = OVXMap.getInstance();

    /**
     * Tears down and/or unregisters OVXPorts mapped to this PhysicalPort. Also
     * sets them to edge, since they don't have things connected to them.
     *
     * @param ppt
     *            PhysicalPort mapped to
     * @param stop
     *            if true, unregister OVXPorts.
     */
    public void deactivateOVXPorts(final PhysicalPort ppt, final boolean stop) {
        /*
         * Check for OVXPorts mapped to this phyport. Expect
         * OVXLinks/SwitchRoutes to be torn down, since dead endpoint =
         * non-recoverable VLink.
         */
        final List<Map<Integer, OVXPort>> vports = ppt.getOVXPorts(null);

        for (final Map<Integer, OVXPort> el : vports) {
            for (final OVXPort vp : el.values()) {
                vp.tearDown();
                if (stop) {
                    vp.unregister();
                }
            }
        }
    }

    /**
     * Boots an OVXPort back up given that it was not administratively down
     * before. Then we can't boot it even if the provided PhysicalPort is up.
     *
     * @param ppt
     *            The PhysicalPort that has boot()ed
     */
    public void activateOVXPorts(final PhysicalPort ppt) {
        /* return all ports mapped to ppt */
        for (final Map<Integer, OVXPort> el : ppt.getOVXPorts(null)) {
            for (final OVXPort vp : el.values()) {
                /*
                 * OVXPort was down before PhyPort was. Don't bring up w/
                 * PhyPort
                 */
                if (vp.isAdminDown()) {
                    OVXStateManager.log
							.info("OVXPort is admininstratively down,"
									+ " must be enabled manually");
                    continue;
                }
                vp.boot();
            }
        }
    }

    /**
     * Tears down and/or unregisters VLinks (OVXLinks, SwitchRoutes) mapped to
     * this PhysicalLink. This method leaves VLinks that have successfully
     * recovered alone.
     *
     * @param plink
     *            PhysicalLink mapped to
     * @param stop
     *            if true, unregister OVXLinks/SwitchRoutes.
     * @throws LinkMappingException
     */
    public void deactivateVLinks(final PhysicalLink plink, final boolean stop) {
        for (final Integer tid : OVXStateManager.map.listVirtualNetworks()
				.keySet()) {
            try {
                /* handle OVXLinks */
                if (OVXStateManager.map.hasOVXLinks(plink, tid)) {
                    final Collection<OVXLink> vlinks = new ArrayList<OVXLink>(
                            OVXStateManager.map.getVirtualLinks(plink, tid));
                    for (final OVXLink vlink : vlinks) {
                        this.handleVLinkDown(vlink, plink, stop);
                    }
                }
            } catch (final LinkMappingException e) {
                OVXStateManager.log
						.warn("No OVXLink associated with PhysicalLink {}-{} for tenant {}",
								tid);
            }
            /* handle SwitchRoutes */
            try {
                if (OVXStateManager.map.hasSwitchRoutes(plink, tid)) {
                    final Collection<SwitchRoute> routes = new HashSet<SwitchRoute>(
                            OVXStateManager.map.getSwitchRoutes(plink, tid));
                    for (final SwitchRoute route : routes) {
                        this.handleVLinkDown(route, plink, stop);
                    }
                }
            } catch (final LinkMappingException e) {
                OVXStateManager.log
						.warn("No SwitchRoute associated with PhysicalLink {} for tenant {}",
								tid);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void handleVLinkDown(final Link vlink, final Component plink,
			final boolean stop) {
        final boolean save = ((Resilient) vlink).tryRecovery(plink);
        if (!save) {
            /* let OVXPort teardown tear down OVXLink. don't do for SwitchRoute */
            if (vlink instanceof OVXLink) {
                ((OVXPort) vlink.getSrcPort()).tearDown();
                ((OVXPort) vlink.getDstPort()).tearDown();
            }
            vlink.tearDown();

            if (stop) {
                if (vlink instanceof OVXLink) {
                    ((OVXPort) vlink.getSrcPort()).unregister();
                    ((OVXPort) vlink.getDstPort()).unregister();
                }
                vlink.unregister();
            }
        }
    }

    /**
     * Boots a VLink (OVXLink/SwitchRoute) back up, given neither end-points of
     * the VLink were administratively down.
     *
     * @param plink
     */
    public void activateVLinks(final PhysicalLink plink) {
        for (final Integer tid : OVXStateManager.map.listVirtualNetworks()
				.keySet()) {
            /* handle OVXLinks */
            try {
                if (OVXStateManager.map.hasOVXLinks(plink, tid)) {
                    final Collection<OVXLink> vlinks = new ArrayList<OVXLink>(
                            OVXStateManager.map.getVirtualLinks(plink, tid));
                    for (final OVXLink vlink : vlinks) {
                        this.handleVLinkUp(vlink, plink);
                    }
                }
            } catch (final LinkMappingException e) {
                OVXStateManager.log
						.warn("No OVXLink associated with PhysicalLink {} for tenant {}",
								plink, tid);
            }
            /* handle SwitchRoutes */
            try {
                if (OVXStateManager.map.hasSwitchRoutes(plink, tid)) {
                    final Collection<SwitchRoute> routes = new HashSet<SwitchRoute>(
                            OVXStateManager.map.getSwitchRoutes(plink, tid));
                    for (final SwitchRoute route : routes) {
                        this.handleVLinkUp(route, plink);
                    }
                }
            } catch (final LinkMappingException e) {
                OVXStateManager.log
						.warn("No SwitchRoute associated with PhysicalLink {} for tenant {}",
								plink, tid);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void handleVLinkUp(final Link vlink, final PhysicalLink plink) {
        final boolean recover = ((Resilient) vlink).tryRevert(plink);
        if (!recover) {
            if (vlink instanceof SwitchRoute) {
                vlink.boot();
            } else if (vlink.getSrcPort().isAdminDown()
                    || vlink.getDstPort().isAdminDown()) {
                OVXStateManager.log
						.info("OVXLink is admininstratively down, must be enabled manually");
                return;
            } else {
                /* Bringing ports up brings OVXLink up */
                ((OVXPort) vlink.getSrcPort()).boot();
                ((OVXPort) vlink.getDstPort()).boot();
            }
        }
    }

}