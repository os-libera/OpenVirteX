/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ResumeOVXNetwork extends ApiHandler<Map<String, Object>> {
    Logger log = LogManager.getLogger(ResumeOVXNetwork.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
	JSONRPC2Response resp = null;

	try {
	    final Number tenantId = HandlerUtils.<Number> fetchField(
		    TenantHandler.TENANT, params, true, null);
	    final boolean result = false;
	    if (result == false) {
		resp = new JSONRPC2Response(false, 0);
	    } else {
		this.log.info("");
		resp = new JSONRPC2Response(true, 0);
	    }

	} catch (final MissingRequiredField e) {
	    resp = new JSONRPC2Response(
		    new JSONRPC2Error(
		            JSONRPC2Error.INVALID_PARAMS.getCode(),
		            this.cmdName()
		                    + ": Unable to create this virtual port in the virtual network : "
		                    + e.getMessage()), 0);
	}
	return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
	return JSONRPC2ParamsType.OBJECT;
    }

}
