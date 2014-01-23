/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectOVXLink;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectOVXRoute;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectHost;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectOVXLink;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectOVXRoute;
import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXBigSwitchRouting;
import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXLinkPath;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXSwitch;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class TenantHandler extends AbstractHandler implements RequestHandler {

	// Tenant keywords
	public final static String CTRLHOST = "controllerAddress";
	public final static String CTRLPORT = "controllerPort";
	public final static String PROTOCOL = "protocol";
	public final static String NETADD = "networkAddress";
	public final static String NETMASK = "mask";
	public static final String TENANT = "tenantId";
	public static final String DPIDS = "dpids";
	public static final String DPID = "dpid";
	public static final String SRC_DPID = "srcDpid";
	public static final String DST_DPID = "dstDpid";
	public static final String LINK = "linkId";
	public static final String SWITCH_ROUTE = "switch_route";
	public static final String PORT = "port";
	public static final String VPORT = "vport";
	public static final String VDPID = "vdpid";
	public static final String SRC_PORT = "srcPort";
	public static final String DST_PORT = "dstPort";
	public static final String PRIORITY = "priority";
	public static final String MAC = "mac";
	public static final String PATH = "path";
	public static final String ALGORITHM = "algorithm";
	public static final String BACKUPS = "backup_num";
	public static final String HOST = "hostId";
	public static final String ROUTE = "routeId";
	public static final String IS_BOOTED = "isBooted";

	@SuppressWarnings({ "serial", "rawtypes" })
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {
		{
			this.put("createNetwork", new CreateOVXNetwork());
			this.put("createSwitch", new CreateOVXSwitch());
			this.put("createPort", new CreateOVXPort());
			this.put("setInternalRouting", new SetOVXBigSwitchRouting());
			this.put("connectHost", new ConnectHost());
			this.put("connectLink", new ConnectOVXLink());
			this.put("setLinkPath", new SetOVXLinkPath());
			this.put("connectRoute", new ConnectOVXRoute());

			this.put("removeNetwork", new RemoveOVXNetwork());
			this.put("removeSwitch", new RemoveOVXSwitch());
			this.put("removePort", new RemoveOVXPort());
			this.put("disconnectHost", new DisconnectHost());
			this.put("disconnectLink", new DisconnectOVXLink());
			this.put("disconnectRoute", new DisconnectOVXRoute());

			this.put("startNetwork", new StartOVXNetwork());
			this.put("startSwitch", new StartOVXSwitch());
			this.put("startPort", new StartOVXPort());
			this.put("stopNetwork", new StopOVXNetwork());
			this.put("stopSwitch", new StopOVXSwitch());
			this.put("stopPort", new StopOVXPort());
		}
	};

	@Override
	public String[] handledRequests() {
		return this.handlers.keySet().toArray(new String[] {});
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSONRPC2Response process(final JSONRPC2Request req,
			final MessageContext ctxt) {

		final ApiHandler m = this.handlers.get(req.getMethod());
		if (m != null) {

			if (m.getType() != JSONRPC2ParamsType.NO_PARAMS
					&& m.getType() != req.getParamsType()) {
				return new JSONRPC2Response(new JSONRPC2Error(
						JSONRPC2Error.INVALID_PARAMS.getCode(), req.getMethod()
						+ " requires: " + m.getType() + "; got: "
						+ req.getParamsType()), req.getID());
			}

			switch (m.getType()) {
			case NO_PARAMS:
				return m.process(null);
			case ARRAY:
				return m.process(req.getPositionalParams());
			case OBJECT:
				return m.process(req.getNamedParams());
			}
		}

		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
	}

}
