package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXFlowEntry;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsReply;

import org.openflow.protocol.OFMatch;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/*
 * FIXME: This needs to fixed, currently it returns incorrect data
 * because the virtualflowtable is actually a physical flowtable.
 * 
 */
public class GetPhysicalFlowtable extends ApiHandler<Map<String, Object>> {

	JSONRPC2Response resp = null;

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		try {
			final List<Map<String, Object>> res = new LinkedList<Map<String, Object>>();
			final Number dpid = HandlerUtils.<Number> fetchField(
					MonitoringHandler.DPID, params, true, null);
			final OVXMap map = OVXMap.getInstance();
			
			final PhysicalSwitch sw = PhysicalNetwork.getInstance().getSwitch(dpid.longValue());
			
			LinkedList<OVXFlowStatisticsReply> flows = new LinkedList<OVXFlowStatisticsReply>();
			for (Integer tid : map.listVirtualNetworks().keySet()) {
				if (sw.getFlowStats(tid) != null)
					flows.addAll(sw.getFlowStats(tid));
			}
			
			for (OVXFlowStatisticsReply frep : flows) {
				OVXFlowMod fm = new OVXFlowMod();
				fm.setActions(frep.getActions());
				fm.setMatch(frep.getMatch());
				res.add(fm.toMap());
			}
			
			

			this.resp = new JSONRPC2Response(res, 0);

		} catch (ClassCastException | MissingRequiredField e) {
			this.resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Unable to fetch virtual topology : "
							+ e.getMessage()), 0);
		} catch (final InvalidDPIDException e) {
			this.resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Unable to fetch virtual topology : "
							+ e.getMessage()), 0);
		}
		
		return this.resp;

	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
