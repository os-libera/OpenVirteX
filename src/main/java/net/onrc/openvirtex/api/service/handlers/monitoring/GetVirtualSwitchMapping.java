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
import net.onrc.openvirtex.exceptions.MissingRequiredField;

public class GetVirtualSwitchMapping extends ApiHandler<Map<String, Object>> {

	JSONRPC2Response resp = null;
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		try {
			Map<String, List<String>> res = new HashMap<String, List<String>>();
			Number tid = HandlerUtils.<Number>fetchField(MonitoringHandler.TENANT, params, true, null);
			OVXMap map = OVXMap.getInstance();
			LinkedList<String> list = null;
			for (OVXSwitch vsw : map.getVirtualNetwork(tid.intValue()).getSwitches()) {
				list = new LinkedList<String>();
				for (PhysicalSwitch psw : map.getPhysicalSwitches(vsw)) 
					list.add(psw.getSwitchName());
				res.put(vsw.getSwitchName(), list);
			}
			resp = new JSONRPC2Response(res, 0);
			
		} catch (ClassCastException | MissingRequiredField | NullPointerException e) {
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
