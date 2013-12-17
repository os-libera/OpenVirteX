/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.host.HostSerializer;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetVirtualHosts extends ApiHandler<Map<String, Object>> {

	@SuppressWarnings("unchecked")
	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		List<Object> result;
		JSONRPC2Response resp = null;
		Number tid = null;
		try {
			tid = HandlerUtils.<Number> fetchField(
					MonitoringHandler.TENANT, params, true, null);
			final OVXNetwork vnet = OVXMap.getInstance().getVirtualNetwork(
					tid.intValue());
			
			// TODO: gson objects can be shared with other methods
			final GsonBuilder gsonBuilder = new GsonBuilder();
			// gsonBuilder.setPrettyPrinting();
			gsonBuilder.registerTypeAdapter(Host.class,
				new HostSerializer());
			final Gson gson = gsonBuilder.create();
			//TODO Fix this GSON crazy shit.
			
			result = gson.fromJson(gson.toJson(vnet.getHosts()), List.class );
			resp = new JSONRPC2Response(result, 0);
		} catch (ClassCastException | MissingRequiredField e) {
			resp = new JSONRPC2Response(
					new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
							this.cmdName() + ": Unable to fetch host list : "
									+ e.getMessage()), 0);
		} catch (NetworkMappingException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid tenantId : " + tid), 0);
		}
		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
