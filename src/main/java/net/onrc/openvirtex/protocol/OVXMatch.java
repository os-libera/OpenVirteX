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
package net.onrc.openvirtex.protocol;

import java.util.HashMap;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;


/**
 * The Class OVXMatch. This class extends the OFMatch class, in order to carry
 * some useful informations for OpenVirteX, as the cookie (used by flowMods
 * messages) and the packet data (used by packetOut messages)
 */
public class OVXMatch {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The cookie. */
    protected long cookie;

    /** The pkt data. */
    protected byte[] pktData;

    OFVersion ofVersion;
    OFFactory ofFactory;
    Match ofMatch;

    /**
     * Instantiates a new void OVXatch.
     */
    public OVXMatch(OFVersion ofVersion) {
        this.ofVersion = ofVersion;
        this.ofFactory = OFFactories.getFactory(ofVersion);
        this.ofMatch = this.ofFactory.matchWildcardAll();

        this.cookie = 0;
        this.pktData = null;
    }

    /**
     * Instantiates a new OVXmatch from an OFMatch instance.
     *
     * @param match
     *            the match
     */
    public OVXMatch(final Match match) {
        /*this.wildcards = match.getWildcards();
        this.inputPort = match.getInputPort();
        this.dataLayerSource = match.getDataLayerSource();
        this.dataLayerDestination = match.getDataLayerDestination();
        this.dataLayerVirtualLan = match.getDataLayerVirtualLan();
        this.dataLayerVirtualLanPriorityCodePoint = match
                .getDataLayerVirtualLanPriorityCodePoint();
        this.dataLayerType = match.getDataLayerType();
        this.networkTypeOfService = match.getNetworkTypeOfService();
        this.networkProtocol = match.getNetworkProtocol();
        this.networkSource = match.getNetworkSource();
        this.networkDestination = match.getNetworkDestination();
        this.transportSource = match.getTransportSource();
        this.transportDestination = match.getTransportDestination();*/
        this.ofMatch = match.createBuilder().build();
        this.cookie = 0;
        this.pktData = null;
    }

    public Match getMatch() { return this.ofMatch; }

    public void  setMatch(Match match ) {
        this.ofMatch = match.createBuilder().build();
    }

    /**
     * Get cookie.
     *
     * @return the cookie
     */
    public long getCookie() {
        return this.cookie;
    }

    /**
     * Set cookie.
     *
     * @param cookie
     *            the cookie
     * @return the oVX match
     */
    public OVXMatch setCookie(final long cookie) {
        this.cookie = cookie;
        return this;
    }

    /**
     * Gets the pkt data.
     *
     * @return the pkt data
     */
    public byte[] getPktData() {
        return this.pktData;
    }

    /**
     * Sets the pkt data.
     *
     * @param pktData
     *            the new pkt data
     */
    public void setPktData(final byte[] pktData) {
        this.pktData = pktData;
    }

    /**
     * Checks if this match belongs to a flow mod (e.g. the cookie is not zero).
     *
     * @return true, if is flow mod
     */
    public boolean isFlowMod() {
        return this.cookie != 0;
    }

    /**
     * Checks if this match belongs to a packet out (e.g. the packet data is not
     * null).
     *
     * @return true, if is packet out
     */
    public boolean isPacketOut() {
        return this.pktData != null;
    }

    /*public static class CIDRToIP {
        public static String cidrToString(final int ip, final int prefix) {
            String str;
            if (prefix >= 32) {
                str = OFMatch.ipToString(ip);
            } else {
                // use the negation of mask to fake endian magic
                final int mask = ~((1 << 32 - prefix) - 1);
                str = OFMatch.ipToString(ip & mask) + "/" + prefix;
            }

            return str;
        }
    }*/

    public HashMap<String, Object> toMap() {

        final HashMap<String, Object> ret = new HashMap<String, Object>();


        if(this.ofMatch.get(MatchField.IN_PORT) != null)
            ret.put(MatchField.IN_PORT.getName(), this.ofMatch.get(MatchField.IN_PORT).toString());

        if(this.ofMatch.get(MatchField.ETH_SRC) != null)
            ret.put(MatchField.ETH_SRC.getName(), this.ofMatch.get(MatchField.ETH_SRC).toString());

        if(this.ofMatch.get(MatchField.ETH_DST) != null)
            ret.put(MatchField.ETH_DST.getName(), this.ofMatch.get(MatchField.ETH_DST).toString());

        if(this.ofMatch.get(MatchField.ETH_TYPE) != null)
            ret.put(MatchField.ETH_TYPE.getName(), this.ofMatch.get(MatchField.ETH_TYPE).toString());

        if(this.ofMatch.get(MatchField.VLAN_VID) != null)
            ret.put(MatchField.VLAN_VID.getName(), this.ofMatch.get(MatchField.VLAN_VID).toString());

        if(this.ofMatch.get(MatchField.VLAN_PCP) != null)
            ret.put(MatchField.VLAN_PCP.getName(), this.ofMatch.get(MatchField.VLAN_PCP).toString());

        if(this.ofMatch.get(MatchField.IPV4_SRC) != null) {
            if(this.ofMatch.isPartiallyMasked(MatchField.IPV4_SRC))
                ret.put(MatchField.IPV4_SRC.getName(), this.ofMatch.getMasked(MatchField.IPV4_SRC).toString());
            else
                ret.put(MatchField.IPV4_SRC.getName(), this.ofMatch.get(MatchField.IPV4_SRC).toString());
        }

        if(this.ofMatch.get(MatchField.IPV4_DST) != null) {
            if(this.ofMatch.isPartiallyMasked(MatchField.IPV4_DST))
                ret.put(MatchField.IPV4_DST.getName(), this.ofMatch.getMasked(MatchField.IPV4_DST).toString());
            else
                ret.put(MatchField.IPV4_DST.getName(), this.ofMatch.get(MatchField.IPV4_DST).toString());
        }

        if(this.ofMatch.get(MatchField.IP_PROTO) != null)
            ret.put(MatchField.IP_PROTO.getName(), this.ofMatch.get(MatchField.IP_PROTO).toString());

        if(this.ofMatch.get(MatchField.TCP_SRC) != null)
            ret.put(MatchField.TCP_SRC.getName(), this.ofMatch.get(MatchField.TCP_SRC).toString());

        if(this.ofMatch.get(MatchField.TCP_DST) != null)
            ret.put(MatchField.TCP_DST.getName(), this.ofMatch.get(MatchField.TCP_DST).toString());

        return ret;
    }

    /**
     * Return an OFAction associated with nw_src
     *
     * @param tenantId
     * @return OFAction or null
     */
    /*public OFAction getNetworkSrcAction(int tenantId) {
        OVXActionNetworkLayerSource srcAct = null;
        if (!this.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
            srcAct = new OVXActionNetworkLayerSource();
            srcAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId, this.networkSource));
        }
        return srcAct;
    }*/

    /**
     * Return an OFAction associated with nw_dst
     *
     * @param tenantId
     * @return OFAction or null
     */
    /*public OFAction getNetworkDstAction(int tenantId) {
        OVXActionNetworkLayerDestination dstAct = null;
        if (!this.getWildcardObj().isWildcarded(Flag.NW_DST)) {
            dstAct = new OVXActionNetworkLayerDestination();
            dstAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId, this.networkDestination));
        }
        return dstAct;
    }*/
}
