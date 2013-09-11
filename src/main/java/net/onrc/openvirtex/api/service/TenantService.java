package net.onrc.openvirtex.api.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;

public class TenantService extends AbstractService {

	private static Logger log = LogManager.getLogger(TenantService.class.getName());
	
	Dispatcher dispatcher = new Dispatcher();
	
	public TenantService() {
		dispatcher.register(new TenantHandler());
	}
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) {
		JSONRPC2Request json = null;
		JSONRPC2Response jsonResp = null;
		try {
			json = parseJSONRequest(request);
			jsonResp = dispatcher.process(json, null);
			jsonResp.setID(json.getID());
		} catch (IOException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), 
					stack2string(e)), 0);
		} catch (JSONRPC2ParseException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), 
					stack2string(e)), 0);
		}
		try {
			writeJSONObject(response, jsonResp);
		} catch (IOException e) {
			log.fatal("Unable to send response: {} ", stack2string(e));
		}
		
	}

}
