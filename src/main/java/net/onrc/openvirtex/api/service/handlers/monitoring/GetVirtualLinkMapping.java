package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

public class GetVirtualLinkMapping extends ApiHandler<Map<String, Object>> {

	JSONRPC2Response resp = null;
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		try {
			Map<Integer, List<Integer>> res = new HashMap<Integer, List<Integer>>();
			Number tid = HandlerUtils.<Number>fetchField(MonitoringHandler.TENANT, params, true, null);
			OVXMap map = OVXMap.getInstance();
			LinkedList<Integer> list = null;
			for (OVXLink vlink : map.getVirtualNetwork(tid.intValue()).getLinkSet()) {
				list = new LinkedList<Integer>();
				for (PhysicalLink link : map.getPhysicalLinks(vlink))
					list.add(link.getLinkId());
				res.put(vlink.getLinkId(), list);
			}
			resp = new JSONRPC2Response(res, 0);
			
		} catch (ClassCastException | MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Unable to fetch virtual topology : "
							+ e.getMessage()), 0);
		}
		
		
		return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
