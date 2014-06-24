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
package net.onrc.openvirtex.elements.address;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

/**
 * Utility class for IP mapping operations. Implements methods rewrite or add
 * actions for IP translation.
 */
public final class IPMapper {
    private static Logger log = LogManager.getLogger(IPMapper.class.getName());

    /**
     * Overrides default constructor to no-op private constructor. Required by
     * checkstyle.
     */
    private IPMapper() {
    }

    public static Integer getPhysicalIp(final Integer tenantId,
			final Integer virtualIP, final String ipfor) {
        final Mappable map = OVXMap.getInstance();
        final OVXIPAddress vip = new OVXIPAddress(tenantId, virtualIP);
        PhysicalIPAddress pip = null;
        try {
            if (map.hasPhysicalIP(vip, tenantId)) {
                pip = map.getPhysicalIP(vip, tenantId);
            } else {
                pip = new PhysicalIPAddress(map.getVirtualNetwork(tenantId)
                        .nextIP(ipfor));
                pip.setTenantId(tenantId);
                IPMapper.log.debug("Adding IP mapping {} -> {} for tenant {}",
						vip, pip, tenantId);
                IPMapper.log.info("Adding IP mapping {} -> {} for tenant {}",
						vip, pip, tenantId);
                map.addIP(pip, vip);
            }
            return pip.getIp();
        } catch (final IndexOutOfBoundException e) {
            IPMapper.log.error(
                    "No available physical IPs for virtual ip {} in tenant {}",
                    vip, tenantId);
        } catch (final NetworkMappingException e) {
            IPMapper.log.error(e);
        } catch (final AddressMappingException e) {
            IPMapper.log.error(
					"Inconsistency in Physical-Virtual mapping : {}", e);
        }
        return 0;
    }

    public static void rewriteMatch(final Integer tenantId, final OFMatch match) {
        match.setNetworkSource(IPMapper.getPhysicalIp(tenantId,
                match.getNetworkSource(), PhysicalIPAddress.IP_FOR_SOURCE));
        match.setNetworkDestination(IPMapper.getPhysicalIp(tenantId,
                match.getNetworkDestination(),
                PhysicalIPAddress.IP_FOR_DESTINATION));
    }

    public static List<OFAction> prependRewriteActions(final Integer tenantId,
            final OFMatch match) {
        final List<OFAction> actions = new LinkedList<OFAction>();
        if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
            final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
            srcAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId,
                    match.getNetworkSource(), PhysicalIPAddress.IP_FOR_SOURCE));
            actions.add(srcAct);
        }
        if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
            final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
            dstAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId,
                    match.getNetworkDestination(),
                    PhysicalIPAddress.IP_FOR_DESTINATION));
            actions.add(dstAct);
        }
        return actions;
    }

    public static List<OFAction> prependUnRewriteActions(final OFMatch match) {
        final List<OFAction> actions = new LinkedList<OFAction>();
        if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
            final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
            srcAct.setNetworkAddress(match.getNetworkSource());
            actions.add(srcAct);
        }
        if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
            final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
            dstAct.setNetworkAddress(match.getNetworkDestination());
            actions.add(dstAct);
        }
        return actions;
    }
}
