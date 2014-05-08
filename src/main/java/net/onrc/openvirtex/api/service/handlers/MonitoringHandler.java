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

import net.onrc.openvirtex.api.service.handlers.monitoring.GetPhysicalFlowtable;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetPhysicalHosts;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetPhysicalTopology;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetSubnet;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualAddressMapping;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualFlowtable;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualHosts;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualLinkMapping;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualSwitchMapping;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualTopology;
import net.onrc.openvirtex.api.service.handlers.monitoring.ListVirtualNetworks;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class MonitoringHandler extends AbstractHandler implements
		RequestHandler {

	public static final String TENANT = "tenantId";
	public static final String MAC = "mac";
	public static final String DPID = "dpid";
	public static final String VDPID = "vdpid";

	@SuppressWarnings({ "serial", "rawtypes" })
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {
		{
			this.put("getPhysicalTopology", new GetPhysicalTopology());
			this.put("listVirtualNetworks", new ListVirtualNetworks());
			this.put("getVirtualTopology", new GetVirtualTopology());
			this.put("getVirtualSwitchMapping", new GetVirtualSwitchMapping());
			this.put("getVirtualLinkMapping", new GetVirtualLinkMapping());
			this.put("getVirtualHosts", new GetVirtualHosts());
			this.put("getPhysicalHosts", new GetPhysicalHosts());
			this.put("getSubnet", new GetSubnet());
			this.put("getVirtualFlowtable", new GetVirtualFlowtable());
			this.put("getPhysicalFlowtable", new GetPhysicalFlowtable());
			this.put("getVirtualAddressMapping", new GetVirtualAddressMapping());
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
