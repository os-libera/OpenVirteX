/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.messages;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.LinkPair;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.routing.SwitchRoute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

public class OVXPortStatus extends OVXMessage implements Virtualizable {

    private final Logger log = LogManager.getLogger(OVXPortStatus.class);

    public OVXPortStatus(OFMessage msg){
        super(msg);
    }

    public OFPortStatus getPortStatus() { return (OFPortStatus)this.getOFMessage(); }

    @Override
    public void virtualize(final PhysicalSwitch sw) {
       // this.log.info("virtualize");


        Mappable map = sw.getMap();
        PhysicalPort p = sw.getPort(this.getPortStatus().getDesc().getPortNo().getShortPortNumber());
        if (p == null) {
            handlePortAdd(sw, p);
            return;
        }

        log.info("Received {} from switch {}", this.getOFMessage().toString(),
                sw.getSwitchId());
        LinkPair<PhysicalLink> pair = p.getLink();
        try {
            Set<Integer> vnets = map.listVirtualNetworks().keySet();
            for (Integer tenantId : vnets) {
                /* handle vLinks/routes containing phyLink to/from this port. */
                if ((pair != null) && (pair.exists())) {
                    handleLinkChange(sw, map, pair, tenantId);
                }
                List<Map<Integer, OVXPort>> vports = p.getOVXPorts(tenantId);
                /* cycle through all OVXPorts for this port. */
                Iterator<Map<Integer, OVXPort>> pItr = vports.iterator();
                while (pItr.hasNext()) {
                    Map<Integer, OVXPort> mp = pItr.next();
                    if (mp == null) {
                        continue;
                    }
                    for (Map.Entry<Integer, OVXPort> pMap : mp.entrySet()) {
                        OVXPort vport = pMap.getValue();
                        if (vport == null) {
                            continue;
                        }
                        if (isReason(OFPortReason.DELETE)) {
                            /* try to remove OVXPort, vLinks, routes */
                            vport.unMapHost();
                            vport.handlePortDelete(this);
                            sw.removePort(p);
                        } else if (isReason(OFPortReason.MODIFY)) {
                            if (isState(OFPortState.LINK_DOWN)) {
                                /* set ports as edge, but don't remove vLinks */
                                vport.handlePortDisable(this);
                            } else if (!isState(OFPortState.LINK_DOWN)
                                    && !p.getOfPort().getState().contains(OFPortState.LINK_DOWN)) {
//!p.getSrcPort().getOfPort().getState().contains(OFPortState.LINK_DOWN)
                                //plink.getSrcPort().getOfPort().getState().size() == 0)
                                /*
                                 * set links to non-edge, if it was previously
                                 * disabled
                                 */
                                vport.handlePortEnable(this);
                            }
                        }
                    }
                }
            }
        } catch (NetworkMappingException | LinkMappingException e) {
            log.warn("Couldn't process reason={} for PortStatus for port {}",
                    this.getPortStatus().getReason().toString(), p.getPortNumber());
            e.printStackTrace();
        }


    }

    /**
     * Adds a new port to the physical switch if add message, quit otherwise.
     *
     * @param sw the physical switch
     * @param p the physical port
     */
    private void handlePortAdd(PhysicalSwitch sw, PhysicalPort p) {
        if (isReason(OFPortReason.ADD)) {
            p = new PhysicalPort(this.getPortStatus().getDesc(), sw, true);
            if (!sw.addPort(p)) {
                log.warn("Could not add new port {} to physical switch {}",
                        p.getPortNumber(), sw.getSwitchId());
            }
            log.info("Added port {} to switch {}", p.getPortNumber(),
                    sw.getSwitchId());
        }
    }

    /**
     * Handles change in internal link state, e.g., a PhysicalPort in, but not at
     * edges of, an OVXLink or SwitchRoute.
     *
     * @param map
     *            Mappable containing global information
     * @param pair
     *            the LinkPair associated with the PhysicalPort
     * @param tid
     *            the tenant ID
     * @throws LinkMappingException
     * @throws NetworkMappingException
     */
    private void handleLinkChange(PhysicalSwitch sw, Mappable map,
                                  LinkPair<PhysicalLink> pair, int tid) throws LinkMappingException,
            NetworkMappingException {
        PhysicalLink plink = pair.getOutLink();

        if (!isState(OFPortState.LINK_DOWN)
                && !plink.getSrcPort().getOfPort().getState().contains(OFPortState.LINK_DOWN)) {

            OVXNetwork net = map.getVirtualNetwork(tid);
            for (OVXLink link : net.getLinks()) {
                link.tryRevert(plink);
            }
            for (OVXSwitch ovxSw : net.getSwitches()) {
                if (ovxSw instanceof OVXBigSwitch) {
                    for (Map<OVXPort, SwitchRoute> routeMap : ((OVXBigSwitch) ovxSw)
                            .getRouteMap().values()) {
                        for (SwitchRoute route : routeMap.values()) {
                            route.tryRevert(plink);
                        }
                    }
                }
            }
        }

        if (map.hasOVXLinks(plink, tid)) {
            List<OVXLink> vlinks = map.getVirtualLinks(plink, tid);
            for (OVXLink vlink : vlinks) {
                if (isReason(OFPortReason.DELETE)) {
                    /* couldn't recover, remove link */
                    if (!vlink.tryRecovery(plink)) {
                        OVXPort vport = vlink.getSrcPort();
                        vport.unMapHost();
                        vport.handlePortDelete(this);
                        sw.removePort(plink.getSrcPort());
                    }
                }
                if (isReason(OFPortReason.MODIFY)) {
                    if (isState(OFPortState.LINK_DOWN)) {
                        /* couldn't recover, remove link */
                        if (!vlink.tryRecovery(plink)) {
                            vlink.getSrcPort().handlePortDisable(this);
                        }
                    } else if (!isState(OFPortState.LINK_DOWN)
                            && !plink.getSrcPort().getOfPort().getState().contains(OFPortState.LINK_DOWN)) {
                        log.debug("enabling OVXLink mapped to port {}");
                        /*
                         * try to switch back to original path, if not just
                         * bring up and hope it's working
                         */
                        if (!vlink.tryRevert(plink)) {
                            vlink.getSrcPort().handlePortEnable(this);
                        }
                    }
                }
            }
        }
        if (map.hasSwitchRoutes(plink, tid)) {
            Set<SwitchRoute> routes = new HashSet<SwitchRoute>(
                    map.getSwitchRoutes(plink, tid));
            for (SwitchRoute route : routes) {
                /*
                 * try to recover, remove route if we fail, but don't send any
                 * stat up
                 */
                if ((isReason(OFPortReason.DELETE))
                        || (isReason(OFPortReason.MODIFY) & isState(OFPortState.LINK_DOWN))) {
                    if (!route.tryRecovery(plink)) {
                        route.getSrcPort().handleRouteDisable(this);
                    }
                }
            }
        }
    }

    public boolean isReason(OFPortReason reason) {
        return this.getPortStatus().getReason() == reason;
    }

    public boolean isState(OFPortState state) {
        return this.getPortStatus().getDesc().getState().contains(state);
    }

    @Override
    public String toString() {
        return "OVXPortStatus: reason["
                + this.getPortStatus().getReason().toString() + "]"
                + " port[" + this.getPortStatus().getDesc().getPortNo().getShortPortNumber() + "]";
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
