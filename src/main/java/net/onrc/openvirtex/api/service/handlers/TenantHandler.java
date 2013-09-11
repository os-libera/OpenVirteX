package net.onrc.openvirtex.api.service.handlers;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXLink;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.SaveConfig;
import net.onrc.openvirtex.api.service.handlers.tenant.StartNetwork;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class TenantHandler extends AbstractHandler implements RequestHandler {

	//Tenant keywords
	public final static String CTRLHOST = "controllerAddress";
	public final static String CTRLPORT = "controllerPort";
	public final static String PROTOCOL = "protocol";
	public final static String NETADD = "networkAddress";
	public final static String NETMASK = "mask";
	public static final String TENANT = "tenantId";
	public static final String DPIDS = "dpids";
	public static final String DPID = "dpid";
	public static final String PORT = "port";
	public static final String MAC = "mac";
	public static final String PATH = "path";
	
	@SuppressWarnings( { "serial", "rawtypes" } )
	HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {{
		put("createNetwork", new CreateOVXNetwork());
		put("createSwitch", new CreateOVXSwitch());
		put("connectHost", new ConnectHost());
		put("createLink", new CreateOVXLink());
		put("startNetwork", new StartNetwork());
		put("saveConfig", new SaveConfig());
	}};
	
	public String[] handledRequests() {
		return handlers.keySet().toArray(new String[]{});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctxt) {

		ApiHandler m = handlers.get(req.getMethod());
		if (m != null) {

			if (m.getType() != JSONRPC2ParamsType.NO_PARAMS && m.getType() != req.getParamsType())
				return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
						req.getMethod() + " requires: " + m.getType() + 
						"; got: " + req.getParamsType()),
						req.getID());

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
