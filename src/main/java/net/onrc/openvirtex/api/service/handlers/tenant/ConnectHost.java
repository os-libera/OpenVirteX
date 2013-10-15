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
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ConnectHost extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(ConnectHost.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
	JSONRPC2Response resp = null;

	try {
	    final Number tenantId = HandlerUtils.<Number> fetchField(
		    TenantHandler.TENANT, params, true, null);
	    final Number dpid = HandlerUtils.<Number> fetchField(
		    TenantHandler.DPID, params, true, null);
	    final Number port = HandlerUtils.<Number> fetchField(
		    TenantHandler.PORT, params, true, null);
	    final String mac = HandlerUtils.<String> fetchField(
		    TenantHandler.MAC, params, true, null);

	    HandlerUtils.isValidTenantId(tenantId.intValue());
	    HandlerUtils.isValidEdgePort(tenantId.intValue(), dpid.longValue(),
		    port.shortValue());
	    final OVXMap map = OVXMap.getInstance();
	    final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
		    .intValue());
	    final MACAddress macAddr = MACAddress.valueOf(mac);
	    final OVXPort edgePort = virtualNetwork.createHost(
		    dpid.longValue(), port.shortValue(), macAddr);
	    if (edgePort == null) {
		resp = new JSONRPC2Response(-1, 0);
	    } else {
		this.log.info(
			"Created edge port {} on virtual switch {} in virtual network {}",
			edgePort.getPortNumber(), edgePort.getParentSwitch()
			.getSwitchId(), virtualNetwork.getTenantId());
		resp = new JSONRPC2Response(edgePort.getPortNumber(), 0);
	    }

	} catch (final MissingRequiredField e) {
	    resp = new JSONRPC2Response(new JSONRPC2Error(
		    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
		    + ": Unable to connect this host to the virtual network : "
		    + e.getMessage()), 0);
	} catch (final InvalidPortException e) {
	    resp = new JSONRPC2Response(new JSONRPC2Error(
		    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
		    + ": Invalid port : " + e.getMessage()), 0);
	} catch (final InvalidTenantIdException e) {
	    resp = new JSONRPC2Response(new JSONRPC2Error(
		    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
		    + ": Invalid tenant id : " + e.getMessage()), 0);
	} catch (IndexOutOfBoundException e) {
	    resp = new JSONRPC2Response(new JSONRPC2Error(
		    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
		    + ": Impossible to create the virtual port, too many ports on this virtual switch : " + e.getMessage()), 0);
	}

	return resp;
    }

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
