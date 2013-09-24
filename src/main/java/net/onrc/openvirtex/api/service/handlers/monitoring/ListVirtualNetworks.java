package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;


/**
 * Gte a list of network/tenant ids
 * 
 * @return list of tenant ids
 */
public class ListVirtualNetworks extends ApiHandler<Object> {

	@Override
	public JSONRPC2Response process(final Object params) {
		JSONRPC2Response resp = null;
		
		Map<Integer, OVXNetwork> nets = OVXMap.getInstance().listVirtualNetworks();
		// JSONRPC2Response wants a List, not a Set
		List<Integer> list = new ArrayList<Integer>(nets.keySet());
		resp = new JSONRPC2Response(list, 0);
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.NO_PARAMS;
	}

}
