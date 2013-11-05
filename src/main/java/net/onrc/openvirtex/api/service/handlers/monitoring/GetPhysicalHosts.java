/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetPhysicalHosts extends ApiHandler<Object> {

	@Override
	public JSONRPC2Response process(Object params) {
		
		JSONRPC2Response resp = null;
		
		try {
			List<Object> hosts = new LinkedList<Object>();
			OVXMap map = OVXMap.getInstance();
			Collection<OVXNetwork> vnets = map.listVirtualNetworks().values();
			
			for (OVXNetwork vnet : vnets) {
				for (Host h : vnet.getHosts())
					hosts.add(h.convertToPhysical());
			}
			
			
			resp = new JSONRPC2Response(hosts, 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(
					new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
							this.cmdName() + ": Unable to fetch host list : "
									+ e.getMessage()), 0);
		} 
		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

}
