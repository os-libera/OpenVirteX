/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.util;

import java.util.HashMap;

import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.exceptions.UnknownActionException;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;

public class OVXUtil {

	public static int NUMBITSNEEDED(int x) {
		int counter = 0;
		while (x != 0) {
			x >>= 1;
			counter++;
		}
		return counter;
	}
	
	public static HashMap<String, Object> actionToMap(OFAction act) throws UnknownActionException {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		
		switch (act.getType()) {
		case OUTPUT:
			OFActionOutput out = (OFActionOutput) act;
			ret.put("type", "OUTPUT");
			ret.put("port", out.getPort());
			break;
		case SET_DL_DST:
			OFActionDataLayerDestination dldst = (OFActionDataLayerDestination) act;
			ret.put("type", "DL_DST");
			ret.put("dl_dst", new MACAddress(dldst.getDataLayerAddress()).toString());
			break;
		case SET_DL_SRC:
			OFActionDataLayerSource dlsrc = (OFActionDataLayerSource) act;
			ret.put("type", "DL_SRC");
			ret.put("dl_src", new MACAddress(dlsrc.getDataLayerAddress()).toString());
			break;
		case SET_NW_DST:
			OFActionNetworkLayerDestination nwdst = (OFActionNetworkLayerDestination) act;
			ret.put("type", "NW_DST");
			ret.put("nw_dst", new PhysicalIPAddress(nwdst.getNetworkAddress()).toSimpleString());
			break;
		case SET_NW_SRC:
			OFActionNetworkLayerSource nwsrc = (OFActionNetworkLayerSource) act;
			ret.put("type", "NW_SRC");
			ret.put("nw_src", new PhysicalIPAddress(nwsrc.getNetworkAddress()).toSimpleString());
			break;
		case SET_NW_TOS:
			OFActionNetworkTypeOfService nwtos = (OFActionNetworkTypeOfService) act;
			ret.put("type", "NW_TOS");
			ret.put("nw_tos", nwtos.getNetworkTypeOfService());
			break;
		case SET_TP_DST:
			OFActionTransportLayerDestination tpdst = (OFActionTransportLayerDestination) act;
			ret.put("type", "TP_DST");
			ret.put("tp_dst", tpdst.getTransportPort());
			break;
		case SET_TP_SRC:
			OFActionTransportLayerSource tpsrc = (OFActionTransportLayerSource) act;
			ret.put("type", "TP_SRC");
			ret.put("tp_src", tpsrc.getTransportPort());
			break;
		case SET_VLAN_ID:
			OFActionVirtualLanIdentifier vlan = (OFActionVirtualLanIdentifier) act;
			ret.put("type", "SET_VLAN");
			ret.put("vlan_id", vlan.getVirtualLanIdentifier());
			break;
		case SET_VLAN_PCP:
			OFActionVirtualLanPriorityCodePoint pcp = (OFActionVirtualLanPriorityCodePoint) act;
			ret.put("type", "SET_VLAN_PCP");
			ret.put("vlan_pcp", pcp.getVirtualLanPriorityCodePoint());
			break;
		case STRIP_VLAN:
			ret.put("type", "STRIP_VLAN");
			break;
		case OPAQUE_ENQUEUE:
			OFActionEnqueue enq = (OFActionEnqueue) act;
			ret.put("type", "ENQUEUE");
			ret.put("queue", enq.getQueueId());
			break;
		case VENDOR:
			ret.put("type", "VENDOR");
			break;	
		default:
			throw new UnknownActionException("Action " + act.getType() + " is unknown.");
				
		}

		return ret;
	}
	
}
