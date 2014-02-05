/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.tenant;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class CreateOVXNetwork extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(CreateOVXNetwork.class.getName());

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {

		JSONRPC2Response resp = null;
		try {
			
			final ArrayList<String> ctrlUrls =  HandlerUtils.<ArrayList<String>>fetchField(
					TenantHandler.CTRLURLS, params, true, null);
			final String netAddress = HandlerUtils.<String> fetchField(
					TenantHandler.NETADD, params, true, null);
			final Number netMask = HandlerUtils.<Number> fetchField(
					TenantHandler.NETMASK, params, true, null);

			for (String ctrl : ctrlUrls) {
				String[] ctrlParts = ctrl.split(":");

				HandlerUtils
				.isControllerAvailable(ctrlParts[1], Integer.parseInt(ctrlParts[2]));
			}
			final IPAddress addr = new OVXIPAddress(netAddress, -1);
			final OVXNetwork virtualNetwork = new OVXNetwork(ctrlUrls, addr, netMask.shortValue());
			virtualNetwork.register();
			this.log.info("Created virtual network {}",
					virtualNetwork.getTenantId());

			Map<String, Object> reply = new HashMap<String, Object>(virtualNetwork.getDBObject());
			resp = new JSONRPC2Response(reply, 0);

		} catch (final MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
					+ ": Unable to create virtual network : "
					+ e.getMessage()), 0);
		} catch (final ControllerUnavailableException e) {
			resp = new JSONRPC2Response(
					new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
							this.cmdName() + ": Controller already in use : "
									+ e.getMessage()), 0);
		} catch (final IndexOutOfBoundException e) {
			resp = new JSONRPC2Response(
					new JSONRPC2Error(
							JSONRPC2Error.INVALID_PARAMS.getCode(),
							this.cmdName()
							+ ": Impossible to create the virtual network, too many networks : "
							+ e.getMessage()), 0);
		}
		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
