package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetVirtualLinkMapping extends ApiHandler<Map<String, Object>> {

	JSONRPC2Response resp = null;
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		try {
			Map<Integer, List<List<Integer>>> res = new HashMap<Integer, List<List<Integer>>>();
			Number tid = HandlerUtils.<Number>fetchField(MonitoringHandler.TENANT, params, true, null);
			OVXMap map = OVXMap.getInstance();
			List<Integer> path = null;
			for (OVXLink vlink : map.getVirtualNetwork(tid.intValue()).getLinkSet()) {
				path = new LinkedList<Integer>();
				for (PhysicalLink link : map.getPhysicalLinks(vlink))
					path.add(link.getLinkId());
				List<List<Integer>> list = res.get(vlink.getLinkId());
				if (list == null) {
					list = new LinkedList<List<Integer>>();
					res.put(vlink.getLinkId(), list);
				}
				list.add(path);
			}
			resp = new JSONRPC2Response(res, 0);
			
		} catch (ClassCastException | MissingRequiredField | 
			NetworkMappingException | LinkMappingException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Unable to fetch virtual topology : "
							+ e.getMessage()), 0);
		} catch (NullPointerException e) {
			resp = new JSONRPC2Response(new HashMap<Integer, List<Integer>>(), 0);
		}
		
		
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
