package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;


import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

public class CreateOVXNetwork extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(CreateOVXNetwork.class.getName());
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		
		JSONRPC2Response resp = null;
		
		try {
			String protocol = HandlerUtils.<String>fetchField(TenantHandler.PROTOCOL, 
					params, true, null);
			String ctrlAddress = HandlerUtils.<String>fetchField(TenantHandler.CTRLHOST, 
					params, true, null);
			Number ctrlPort = HandlerUtils.<Number>fetchField(TenantHandler.CTRLPORT, 
					params, true, null);
			String netAddress = HandlerUtils.<String>fetchField(TenantHandler.NETADD, 
					params, true, null);
			Number netMask = HandlerUtils.<Number>fetchField(TenantHandler.NETMASK, 
					params, true, null);
			
			
			HandlerUtils.isControllerAvailable(ctrlAddress, ctrlPort.intValue());
			final IPAddress addr = new OVXIPAddress(netAddress, -1); 
			final OVXNetwork virtualNetwork = new OVXNetwork(protocol, ctrlAddress,
			        ctrlPort.intValue(), addr, netMask.shortValue());
			virtualNetwork.register();
			log.info("Created virtual network {}",
			        virtualNetwork.getTenantId());
			
			resp = new JSONRPC2Response(virtualNetwork.getTenantId(), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Unable to create virtual network : " + e.getMessage()), 0);
		} catch (ControllerUnavailableException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": Controller already in use : " + e.getMessage()), 0);
		}
			
		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	

}
