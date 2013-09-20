package net.onrc.openvirtex.api.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;

public class MonitoringService extends AbstractService {

	static Logger log = LogManager.getLogger(MonitoringService.class
			.getName());
	
	Dispatcher dispatcher = new Dispatcher();
	
	
	public MonitoringService() {
		this.dispatcher.register(new MonitoringHandler());
	}
	
	@Override
	public void handle(final HttpServletRequest request,
			final HttpServletResponse response) {
		JSONRPC2Request json = null;
		JSONRPC2Response jsonResp = null;
		try {
			json = this.parseJSONRequest(request);
			jsonResp = this.dispatcher.process(json, null);
			jsonResp.setID(json.getID());
		} catch (final IOException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.PARSE_ERROR.getCode(),
					AbstractService.stack2string(e)), 0);
		} catch (final JSONRPC2ParseException e) {
			jsonResp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.PARSE_ERROR.getCode(),
					AbstractService.stack2string(e)), 0);
		}
		try {
			this.writeJSONObject(response, jsonResp);
		} catch (final IOException e) {
			MonitoringService.log.fatal("Unable to send response: {} ",
					AbstractService.stack2string(e));
		}

	}
	
	

}
