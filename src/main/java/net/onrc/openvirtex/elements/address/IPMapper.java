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
package net.onrc.openvirtex.elements.address;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.messages.actions.OVXActionSetNwSrc;
import net.onrc.openvirtex.messages.actions.OVXActionSetNwDst;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;


/**
 * Utility class for IP mapping operations. Implements methods
 * rewrite or add actions for IP translation.
 */
public final class IPMapper {
    private static Logger log = LogManager.getLogger(IPMapper.class.getName());

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private IPMapper() {
    }

    public static Integer getPhysicalIp(Integer tenantId, Integer virtualIP) {

        final Mappable map = OVXMap.getInstance();
        final OVXIPAddress vip = new OVXIPAddress(tenantId, virtualIP);
        try {
            PhysicalIPAddress pip;
            if (map.hasPhysicalIP(vip, tenantId)) {
                pip = map.getPhysicalIP(vip, tenantId);

                log.debug("tenantId[" + tenantId + "] has " + vip.toString()
                + " -> " + pip.toString());

            } else {
                pip = new PhysicalIPAddress(map.getVirtualNetwork(tenantId)
                        .nextIP());
                log.info("Adding IP mapping {} -> {} for tenant {}", vip, pip,
                        tenantId);
                map.addIP(pip, vip);
            }
            return pip.getIp();
        } catch (IndexOutOfBoundException e) {
            log.error(
                    "No available physical IPs for virtual ip {} in tenant {}",
                    vip, tenantId);
        } catch (NetworkMappingException e) {
            log.error(e);
        } catch (AddressMappingException e) {
            log.error("Inconsistency in Physical-Virtual mapping : {}", e);
        }
        return 0;
    }

    public static synchronized Match rewriteMatch(final Integer tenantId, final Match match) {
        Match temp = match.createBuilder().build();

        if(match.get(MatchField.IPV4_SRC) != null)
        {
            temp = OVXMessageUtil.updateMatch(temp,
                    temp.createBuilder()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC,
                                    IPv4Address.of(getPhysicalIp(tenantId, match.get(MatchField.IPV4_SRC).getInt())))
                            .build());
        }

        if(match.get(MatchField.IPV4_DST) != null)
        {
            temp = OVXMessageUtil.updateMatch(temp,
                    temp.createBuilder()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_DST,
                                    IPv4Address.of(getPhysicalIp(tenantId, match.get(MatchField.IPV4_DST).getInt())))
                    .build());
        }
        return temp;
    }


    public static List<OFAction> prependRewriteActions(final Integer tenantId,
                                                       final Match match) {
        if(match.getVersion() == OFVersion.OF_10)
            return prependRewriteActionsVer10(tenantId, match);
        else
            return prependRewriteActionsVer13(tenantId, match);


    }

    public static List<OFAction> prependRewriteActionsVer13(final Integer tenantId,
                                                            final Match match) {
        final List<OFAction> actions = new LinkedList<OFAction>();

        OFFactory factory = OFFactories.getFactory(match.getVersion());

        if (match.get(MatchField.IPV4_SRC) != null) {
            if ( match.get(MatchField.IPV4_SRC).getInt() != 0) {
                OFActionSetField ofActionSetField  = factory.actions().buildSetField()
                        .setField(factory.oxms().ipv4Src(IPv4Address.of(
                                getPhysicalIp(tenantId,
                                        match.get(MatchField.IPV4_SRC).getInt()))))
                        .build();
                actions.add(ofActionSetField);
            }
        }

        if (match.get(MatchField.IPV4_DST) != null) {
            if(match.get(MatchField.IPV4_DST).getInt() != 0) {
                OFActionSetField ofActionSetField = factory.actions().buildSetField()
                        .setField(factory.oxms().ipv4Dst(IPv4Address.of(
                                getPhysicalIp(tenantId,
                                        match.get(MatchField.IPV4_DST).getInt()))))
                        .build();
                actions.add(ofActionSetField);
            }
        }
        return actions;
    }


    public static List<OFAction> prependRewriteActionsVer10(final Integer tenantId,
                                                       final Match match) {
        final List<OFAction> actions = new LinkedList<OFAction>();

        OFActions action = OFFactories.getFactory(match.getVersion()).actions();

        if (match.get(MatchField.IPV4_SRC) != null) {
            if ( match.get(MatchField.IPV4_SRC).getInt() != 0) {
                OFActionSetNwSrc ofActionSetNwSrc = action.buildSetNwSrc()
                        .setNwAddr(IPv4Address.of(getPhysicalIp(tenantId,
                                match.get(MatchField.IPV4_SRC).getInt())))
                        .build();
                //final OVXActionSetNwSrc srcAct = new OVXActionSetNwSrc(ofActionSetNwSrc);
                actions.add(ofActionSetNwSrc);
            }
        }

        if (match.get(MatchField.IPV4_DST) != null) {
            if(match.get(MatchField.IPV4_DST).getInt() != 0) {
                OFActionSetNwDst ofActionSetNwDst = action.buildSetNwDst()
                        .setNwAddr(IPv4Address.of(getPhysicalIp(tenantId,
                                match.get(MatchField.IPV4_DST).getInt())))
                        .build();
                //final OVXActionSetNwDst dstAct = new OVXActionSetNwDst(ofActionSetNwDst);
                actions.add(ofActionSetNwDst);
            }
        }
        return actions;
    }

    public static List<OFAction> prependUnRewriteActions(final Match match) {
        if(match.getVersion() == OFVersion.OF_10)
            return prependUnRewriteActionsVer10(match);
        else
            return prependUnRewriteActionsVer13(match);

    }
    public static List<OFAction> prependUnRewriteActionsVer13(final Match match) {
        final List<OFAction> actions = new LinkedList<OFAction>();

        OFFactory factory = OFFactories.getFactory(match.getVersion());

        if (match.get(MatchField.IPV4_SRC) != null) {
            if(match.get(MatchField.IPV4_SRC).getInt() != 0) {
                OFActionSetField ofActionSetField  = factory.actions().buildSetField()
                        .setField(factory.oxms().ipv4Src(match.get(MatchField.IPV4_SRC)))
                        .build();
                actions.add(ofActionSetField);
            }
        }

        if (match.get(MatchField.IPV4_DST) != null) {
            if(match.get(MatchField.IPV4_DST).getInt() != 0) {
                OFActionSetField ofActionSetField  = factory.actions().buildSetField()
                        .setField(factory.oxms().ipv4Dst(match.get(MatchField.IPV4_DST)))
                        .build();
                actions.add(ofActionSetField);
            }
        }
        return actions;
    }

    public static List<OFAction> prependUnRewriteActionsVer10(final Match match) {
        final List<OFAction> actions = new LinkedList<OFAction>();

        OFActions action = OFFactories.getFactory(match.getVersion()).actions();


        if (match.get(MatchField.IPV4_SRC) != null) {
            if(match.get(MatchField.IPV4_SRC).getInt() != 0) {
                OFActionSetNwSrc ofActionSetNwSrc = action.buildSetNwSrc()
                        .setNwAddr(match.get(MatchField.IPV4_SRC))
                        .build();
//                final OVXActionSetNwSrc srcAct = new OVXActionSetNwSrc(ofActionSetNwSrc);
                actions.add(ofActionSetNwSrc);
            }
        }

        if (match.get(MatchField.IPV4_DST) != null) {
            if(match.get(MatchField.IPV4_DST).getInt() != 0) {
                OFActionSetNwDst ofActionSetNwDst = action.buildSetNwDst()
                        .setNwAddr(match.get(MatchField.IPV4_DST))
                        .build();
//                final OVXActionSetNwDst dstAct = new OVXActionSetNwDst(ofActionSetNwDst);
                actions.add(ofActionSetNwDst);
            }
        }
         return actions;
    }
}
