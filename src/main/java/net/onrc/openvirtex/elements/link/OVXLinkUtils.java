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
package net.onrc.openvirtex.elements.link;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.NetworkMappingException;


import net.onrc.openvirtex.messages.OVXMessageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;

/**
 * This class provides some useful methods to encapsulate/decapsulate the
 * virtual link identifiers (tenantId, linkId, flowId) inside the packet fields
 * (MAC addresses or VLAN).
 */
public class OVXLinkUtils {

    private static Logger log = LogManager.getLogger(OVXLinkUtils.class.getName());
    private Integer tenantId;
    private Integer linkId;
    private Integer flowId;
    private MacAddress srcMac;
    private MacAddress dstMac;
    private Short vlan;

    /**
     * Instantiates a new link utils instance. Never called by external classes.
     */
    protected OVXLinkUtils() {
        this.tenantId = 0;
        this.linkId = 0;
        this.flowId = 0;
        this.srcMac = null;
        this.dstMac = null;
        this.vlan = 0;
    }

    /**
     * Gets the integer value from a given Bitset.
     *
     * @param bitSet
     *            the bitset
     * @return the integer
     */
    private static int bitSetToInt(final BitSet bitSet) {
        int bitInteger = 0;
        for (int i = 0; i < 32; i++) {
            if (bitSet.get(i)) {
                bitInteger |= 1 << i;
            }
        }
        return bitInteger;
    }

    /**
     * Instantiates a new link utils instance from the MAC addresses couple.
     * Automatically decapsulate and set tenantId, linkId and flowId from the
     * parameters given.
     *
     * @param srcMac
     *            the src mac
     * @param dstMac
     *            the dst mac
     */
    public OVXLinkUtils(final MacAddress srcMac, final MacAddress dstMac) {
        this();
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        final int vNets = OpenVirteXController.getInstance()
                .getNumberVirtualNets();
        final MacAddress mac = MacAddress.of(
                (srcMac.getLong() & 0xFFFFFF) << 24 | dstMac.getLong() & 0xFFFFFF);
                //.valueOf((srcMac.toLong() & 0xFFFFFF) << 24 | dstMac.toLong()
                  //      & 0xFFFFFF);
        this.tenantId = (int) (mac.getLong() >> 48 - vNets);
        final BitSet bmask = new BitSet((48 - vNets) / 2);
        for (int i = bmask.nextClearBit(0); i < (48 - vNets) / 2; i = bmask
                .nextClearBit(i + 1)) {
            bmask.set(i);
        }
        final int mask = OVXLinkUtils.bitSetToInt(bmask);
        this.linkId = (int) (mac.getLong() >> (48 - vNets) / 2) & mask;
        this.flowId = (int) mac.getLong() & mask;
        this.vlan = 0;
    }

    /**
     * Instantiates a new link utils from tenantId, linkId and flowId.
     * Automatically encapsulate and set these values in the MAC addresses and
     * in the VLAN.
     *
     * @param tenantId
     *            the tenant id
     * @param linkId
     *            the link id
     * @param flowId
     *            the flow id
     */
    public OVXLinkUtils(final Integer tenantId, final Integer linkId,
                        final Integer flowId) {
        this();
        this.tenantId = tenantId;
        this.linkId = linkId;
        this.flowId = flowId;
        final int vNets = OpenVirteXController.getInstance()
                .getNumberVirtualNets();
        final MacAddress mac = MacAddress.of(
                tenantId.longValue() << 48 - vNets
                        | linkId.longValue() << (48 - vNets) / 2
                        | flowId.longValue());
//                .valueOf(tenantId.longValue() << 48 - vNets
//                        | linkId.longValue() << (48 - vNets) / 2
//                        | flowId.longValue());
        final Long src = mac.getLong() >> 24 & 0xFFFFFF;
        final Long dst = mac.getLong() & 0xFFFFFF;
        this.srcMac = MacAddress.of((long) 0xa42305 << 24 | src);
        this.dstMac = MacAddress.of((long) 0xa42305 << 24 | dst);
        // TODO: encapsulate the values in the vlan too
        this.vlan = 0;
    }

    /**
     * Checks if the link utils instance is valid. To be valid, the instance has
     * to have tenantId, linkId and flowId set. Moreover, both MAC addresses or
     * the VLAN field has to be set too.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (this.tenantId != 0 && this.linkId != 0 && this.flowId != 0) {
            if (this.vlan != 0 || this.srcMac != null && this.dstMac != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the tenant id.
     *
     * @return the tenant id
     */
    public Integer getTenantId() {
        return this.tenantId;
    }

    /**
     * Gets the link id.
     *
     * @return the link id
     */
    public Integer getLinkId() {
        return this.linkId;
    }

    /**
     * Gets the flow id.
     *
     * @return the flow id
     */
    public Integer getFlowId() {
        return this.flowId;
    }

    /**
     * Gets the source MAC address.
     *
     * @return the source MAC
     */
    public MacAddress getSrcMac() {
        return this.srcMac;
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC
     */
    public MacAddress getDstMac() {
        return this.dstMac;
    }

    /**
     * Gets the VLAN.
     *
     * @return the VLAN
     */
    public Short getVlan() {
        return this.vlan;
    }

    /**
     * Gets the original MAC addresses in a list.
     *
     * @return list of original MAC addresses
     * @throws NetworkMappingException
     *             if the tenant ID is invalid
     */
    public LinkedList<MacAddress> getOriginalMacAddresses()
            throws NetworkMappingException {
        final LinkedList<MacAddress> macList = OVXMap.getInstance()
                .getVirtualNetwork(this.tenantId).getFlowManager()
                .getFlowValues(this.flowId);
        return macList;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "tenantId = " + this.tenantId + ", linkId = " + this.linkId
                + ", flowId = " + this.flowId + ", srcMac = " + this.srcMac
                + ", dstMac = " + this.dstMac + ", vlan = " + this.vlan;
    }

    /**
     * Rewrites the given match according to the current instance.
     *
     * @param match
     *            the OpenFlow match
     */
    public Match rewriteMatch(final Match match) {
        final OVXLinkField linkField = OpenVirteXController.getInstance()
                .getOvxLinkField();
        if (linkField == OVXLinkField.MAC_ADDRESS) {
            return OVXMessageUtil.updateMatch(match,
                    match.createBuilder()
                            .setExact(MatchField.ETH_SRC, this.getSrcMac())
                            .setExact(MatchField.ETH_DST, this.getDstMac())
                            .build());
        } else if (linkField == OVXLinkField.VLAN) {
            return OVXMessageUtil.updateMatch(match,
                    match.createBuilder()
                            .setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofRawVid(this.getVlan()))
                            .build());
        }
        return match;
    }

    /**
     * Gets a list of actions based on the current instance.
     *
     * @return list of actions
     */
    public List<OFAction> setLinkFields(final OFVersion ofv) {
        if(ofv == OFVersion.OF_10)
            return setLinkFieldsVer10();
        else
            return setLinkFieldsVer13();

    }

    public List<OFAction> setLinkFieldsVer10() {
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();

        OFActions action = OFFactories.getFactory(OFVersion.OF_10).actions();

        if (linkField == OVXLinkField.MAC_ADDRESS) {
            OFActionSetDlSrc setDlSrc = action.buildSetDlSrc()
                    .setDlAddr(this.getSrcMac())
                    .build();
            actions.add(setDlSrc);

            OFActionSetDlDst setDlDst = action.buildSetDlDst()
                    .setDlAddr(this.getDstMac())
                    .build();
            actions.add(setDlDst);
        } else if (linkField == OVXLinkField.VLAN) {
            OFActionSetVlanVid setVlanVid = action.buildSetVlanVid()
                    .setVlanVid(VlanVid.ofVlan((int)this.getVlan()))
                    .build();
            actions.add(setVlanVid);
        }
        return actions;
    }

    public List<OFAction> setLinkFieldsVer13() {
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();

        OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

        if (linkField == OVXLinkField.MAC_ADDRESS) {
            OFActionSetField ofActionSetField = factory.actions().buildSetField()
                    .setField(factory.oxms().ethSrc(this.getSrcMac()))
                    .build();
            actions.add(ofActionSetField);

            ofActionSetField = factory.actions().buildSetField()
                    .setField(factory.oxms().ethDst(this.getDstMac()))
                    .build();
            actions.add(ofActionSetField);
        } else if (linkField == OVXLinkField.VLAN) {
            OFActionSetField ofActionSetField = factory.actions().buildSetField()
                    .setField(factory.oxms().vlanVid(OFVlanVidMatch.ofRawVid(this.getVlan())))
                    .build();

            actions.add(ofActionSetField);
        }
        return actions;
    }

    /**
     * Gets a list of actions based on the original MAC addresses.
     *
     * @param skipSrcMac Skip rewriting the source MAC address.
     * @param skipDstMac Skip rewriting the destination MAC address.
     * @return list of actions
     */
    public List<OFAction> unsetLinkFields(final boolean skipSrcMac, final boolean skipDstMac, final OFVersion ofv) {
        if(ofv == OFVersion.OF_10)
            return unsetLinkFieldsVer10(skipSrcMac, skipDstMac);
        else
            return unsetLinkFieldsVer13(skipSrcMac, skipDstMac);
    }

    public List<OFAction> unsetLinkFieldsVer13(final boolean skipSrcMac, final boolean skipDstMac) {
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();

        if (linkField == OVXLinkField.MAC_ADDRESS) {
            LinkedList<MacAddress> macList;
            try {
                macList = this.getOriginalMacAddresses();
                if (!skipSrcMac) {
                    OFActionSetField ofActionSetField = factory.actions().buildSetField()
                            .setField(factory.oxms().ethSrc(macList.get(0)))
                            .build();
                    actions.add(ofActionSetField);
                }
                if (!skipDstMac) {
                    OFActionSetField ofActionSetField = factory.actions().buildSetField()
                            .setField(factory.oxms().ethDst(macList.get(1)))
                            .build();
                    actions.add(ofActionSetField);
                }
            } catch (NetworkMappingException e) {
                OVXLinkUtils.log.error("Unable to restore actions: " + e);
            }
        } else {
            if (linkField == OVXLinkField.VLAN) {
                OVXLinkUtils.log.warn("Unable to restore actions, VLANs not supported");
            }
        }

        return actions;

    }


    public List<OFAction> unsetLinkFieldsVer10(final boolean skipSrcMac, final boolean skipDstMac) {
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_10);
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();

        OFActions action = factory.actions();

        if (linkField == OVXLinkField.MAC_ADDRESS) {
            LinkedList<MacAddress> macList;
            try {
                macList = this.getOriginalMacAddresses();
                if (!skipSrcMac) {
                    OFActionSetDlSrc setDlSrc = action.buildSetDlSrc()
                            .setDlAddr(macList.get(0))
                            .build();
                    actions.add(setDlSrc);
                }
                if (!skipDstMac) {
                    OFActionSetDlDst setDlDst = action.buildSetDlDst()
                            .setDlAddr(macList.get(1))
                            .build();
                    actions.add(setDlDst);
                }
            } catch (NetworkMappingException e) {
                OVXLinkUtils.log.error("Unable to restore actions: " + e);
            }
        } else {
            if (linkField == OVXLinkField.VLAN) {
                OVXLinkUtils.log.warn("Unable to restore actions, VLANs not supported");
            }
        }

        return actions;
    }
}
