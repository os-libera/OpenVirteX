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
package net.onrc.openvirtex.api.service.handlers;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.tenant.AddController;
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
	public final static String CTRLURLS = "controllerUrls";
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
			this.put("addControllers", new AddController());
			
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
	public JSONRPC2Response process(final JSONRPC2Request req,
			final MessageContext ctxt) {
		return super.process(this.handlers, req, ctxt);
	}
}
