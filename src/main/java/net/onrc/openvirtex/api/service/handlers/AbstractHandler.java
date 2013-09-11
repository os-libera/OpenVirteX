package net.onrc.openvirtex.api.service.handlers;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

public abstract class AbstractHandler {
	
	public abstract String[] handledRequests();

	
	public abstract JSONRPC2Response process(JSONRPC2Request req, MessageContext ctxt);
}
