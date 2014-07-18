 package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import junit.framework.TestCase;

public class MonitoringAPICalls extends TestCase{
	
	public JSONRPC2Response getVirtualLinkMapping(final Integer tenantId) {
		return this.getVirtualLinkMapping(tenantId, null);
	}
	
	public JSONRPC2Response getVirtualLinkMapping(final Integer tenantId,
			final Integer linkId) {
		final GetVirtualLinkMapping gvlm = new GetVirtualLinkMapping();
		
		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(MonitoringHandler.TENANT, tenantId);
				this.put(MonitoringHandler.LINK, linkId);
			}
		};
		
		return gvlm.process(request);
	}
	
	public JSONRPC2Response getVirtualSwitchMapping(final Integer tenantId) {
		return this.getVirtualSwitchMapping(tenantId, null);
	}
	
	public JSONRPC2Response getVirtualSwitchMapping(final Integer tenantId,
			final Long dpid) {
		final GetVirtualSwitchMapping gvsm = new GetVirtualSwitchMapping();
		
		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(MonitoringHandler.TENANT, tenantId);
				this.put(MonitoringHandler.VDPID, dpid);
			}
		};
		
		return gvsm.process(request);
	}
}
