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
package net.onrc.openvirtex.protocol;

import java.util.HashMap;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.openflow.util.U8;

/**
 * The Class OVXMatch. This class extends the OFMatch class, in order to carry
 * some useful informations for OpenVirteX, as the cookie (used by flowMods
 * messages) and the packet data (used by packetOut messages)
 */
public class OVXMatch extends OFMatch {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The cookie. */
    protected long cookie;

    /** The pkt data. */
    protected byte[] pktData;

    /**
     * Instantiates a new void OVXatch.
     */
    public OVXMatch() {
        super();
        this.cookie = 0;
        this.pktData = null;
    }

    /**
     * Instantiates a new OVXmatch from an OFMatch instance.
     *
     * @param match
     *            the match
     */
    public OVXMatch(final OFMatch match) {
        this.wildcards = match.getWildcards();
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
        this.transportDestination = match.getTransportDestination();
        this.cookie = 0;
        this.pktData = null;
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

    public static class CIDRToIP {
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
    }

    public HashMap<String, Object> toMap() {

        final HashMap<String, Object> ret = new HashMap<String, Object>();

        ret.put("wildcards", this.wildcards);

        // l1
        if ((this.wildcards & OFMatch.OFPFW_IN_PORT) == 0) {
            ret.put(OFMatch.STR_IN_PORT, U16.f(this.inputPort));
        }

        // l2
        if ((this.wildcards & OFMatch.OFPFW_DL_DST) == 0) {
            ret.put(OFMatch.STR_DL_DST,
                    HexString.toHexString(this.dataLayerDestination));
        }

        if ((this.wildcards & OFMatch.OFPFW_DL_SRC) == 0) {
            ret.put(OFMatch.STR_DL_SRC,
                    HexString.toHexString(this.dataLayerSource));
        }

        if ((this.wildcards & OFMatch.OFPFW_DL_TYPE) == 0) {
            ret.put(OFMatch.STR_DL_TYPE, U16.f(this.dataLayerType));
        }

        if ((this.wildcards & OFMatch.OFPFW_DL_VLAN) == 0) {
            ret.put(OFMatch.STR_DL_VLAN, U16.f(this.dataLayerVirtualLan));
        }

        if ((this.wildcards & OFMatch.OFPFW_DL_VLAN_PCP) == 0) {
            ret.put(OFMatch.STR_DL_VLAN_PCP,
                    U8.f(this.dataLayerVirtualLanPriorityCodePoint));
        }

        // l3
        if (this.getNetworkDestinationMaskLen() > 0) {
            ret.put(OFMatch.STR_NW_DST,
                    CIDRToIP.cidrToString(this.networkDestination,
                            this.getNetworkDestinationMaskLen()));
        }

        if (this.getNetworkSourceMaskLen() > 0) {
            ret.put(OFMatch.STR_NW_SRC,
                    CIDRToIP.cidrToString(this.networkSource,
                            this.getNetworkSourceMaskLen()));
        }

        if ((this.wildcards & OFMatch.OFPFW_NW_PROTO) == 0) {
            ret.put(OFMatch.STR_NW_PROTO, this.networkProtocol);
        }

        if ((this.wildcards & OFMatch.OFPFW_NW_TOS) == 0) {
            ret.put(OFMatch.STR_NW_TOS, this.networkTypeOfService);
        }

        // l4
        if ((this.wildcards & OFMatch.OFPFW_TP_DST) == 0) {
            ret.put(OFMatch.STR_TP_DST, this.transportDestination);
        }

        if ((this.wildcards & OFMatch.OFPFW_TP_SRC) == 0) {
            ret.put(OFMatch.STR_TP_SRC, this.transportSource);
        }

        return ret;
    }

	/**
	 * Return an OFAction associated with nw_src
	 *
	 * @param tenantId
	 * @return OFAction or null
	 */
	public OFAction getNetworkSrcAction(int tenantId) {
		OVXActionNetworkLayerSource srcAct = null;
		if (!this.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
			srcAct = new OVXActionNetworkLayerSource();
			srcAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId, this.networkSource));
		}
		return srcAct;
	}

	/**
	 * Return an OFAction associated with nw_dst
	 *
	 * @param tenantId
	 * @return OFAction or null
	 */
	public OFAction getNetworkDstAction(int tenantId) {
		OVXActionNetworkLayerDestination dstAct = null;
		if (!this.getWildcardObj().isWildcarded(Flag.NW_DST)) {
			dstAct = new OVXActionNetworkLayerDestination();
			dstAct.setNetworkAddress(IPMapper.getPhysicalIp(tenantId, this.networkDestination));
		}
		return dstAct;
	}
}
