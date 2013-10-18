package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXFlowEntry;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

import org.openflow.protocol.OFMatch;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/*
 * FIXME: This needs to fixed, currently it returns incorrect data
 * because the virtualflowtable is actually a physical flowtable.
 * 
 */
public class GetVirtualFlowtable extends ApiHandler<Map<String, Object>> {

	JSONRPC2Response resp = null;

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		try {
			final List<Map<String, Object>> res = new LinkedList<Map<String, Object>>();
			final Number tid = HandlerUtils.<Number> fetchField(
					MonitoringHandler.TENANT, params, true, null);
			final Number dpid = HandlerUtils.<Number> fetchField(
					MonitoringHandler.DPID, params, true, null);
			final OVXMap map = OVXMap.getInstance();
			final OVXSwitch vsw = map.getVirtualNetwork(tid.intValue())
					.getSwitch(dpid.longValue());
			final Set<OVXFlowEntry> flows = vsw.getFlowTable().getFlowTable();
			for (final OVXFlowEntry flow : flows) {
				final Map<String, Object> entry = flow.toMap();
				this.translate(entry, map, tid.intValue());
				res.add(entry);
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

	private void translate(final Map<String, Object> entry, final OVXMap map,
			final Integer tid) {
		if (entry.containsKey(OFMatch.STR_NW_SRC)) {
			entry.put(
					OFMatch.STR_NW_SRC,
					map.getVirtualIP(
							new PhysicalIPAddress((String) entry
									.get(OFMatch.STR_NW_SRC))).toSimpeString());
		}
		if (entry.containsKey(OFMatch.STR_NW_DST)) {
			entry.put(
					OFMatch.STR_NW_DST,
					map.getVirtualIP(
							new PhysicalIPAddress((String) entry
									.get(OFMatch.STR_NW_DST))).toSimpeString());
		}

	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}
