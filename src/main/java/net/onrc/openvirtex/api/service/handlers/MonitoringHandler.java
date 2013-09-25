package net.onrc.openvirtex.api.service.handlers;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.monitoring.GetPhysicalTopology;
import net.onrc.openvirtex.api.service.handlers.monitoring.GetVirtualTopology;
import net.onrc.openvirtex.api.service.handlers.monitoring.ListHosts;
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

	@SuppressWarnings({ "serial", "rawtypes" })
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {
		{
			this.put("getPhysicalTopology", new GetPhysicalTopology());
			this.put("listVirtualNetworks", new ListVirtualNetworks());
			this.put("getVirtualTopology", new GetVirtualTopology());
			this.put("getVirtualSwitchMapping", null);
			this.put("getVirtualLinkMapping", null);
			this.put("listHosts", new ListHosts());
			this.put("getSubnet", null);
			this.put("removePhysicalLink", null);
			this.put("getVirtualFlowTable", null);
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
