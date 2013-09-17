package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.HashMap;
import java.util.List;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import junit.framework.TestCase;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;

public class AbstractAPICalls extends TestCase {
	
    
	public JSONRPC2Response createNetwork() {
		return this.createNetwork(6633);
	}
	
    public JSONRPC2Response createNetwork(final Integer port) {
    	CreateOVXNetwork cn = new CreateOVXNetwork();
    	
    	@SuppressWarnings("serial")
		HashMap<String, Object> request = new HashMap<String, Object>() {{
    		put(TenantHandler.PROTOCOL, "tcp");
    		put(TenantHandler.CTRLHOST, "localhost");
    		put(TenantHandler.CTRLPORT, port);
    		put(TenantHandler.NETADD, "10.0.0.0");
    		put(TenantHandler.NETMASK, 24);
    	}};
    	
    	return cn.process(request);
    	
    	
    	
    }
    
    public JSONRPC2Response createSwitch(final Integer tenantId, final List<String> dpids) {
    	OVXMap.getInstance();
    	CreateOVXSwitch cs = new CreateOVXSwitch();
    	
    	@SuppressWarnings("serial")
		HashMap<String, Object> request = new HashMap<String, Object>() {{
    		put(TenantHandler.TENANT, tenantId);
    		put(TenantHandler.DPIDS, dpids);
    	}};
    	
    	return cs.process(request);
    	
    	
    }
    
    
    
    
    public JSONRPC2Response connectHost(final Integer tenantId, final Long dpid, 
    		final Short port, final String mac) {
    	
    	ConnectHost ch = new ConnectHost();
    	
    	@SuppressWarnings("serial")
		HashMap<String, Object> request = new HashMap<String, Object>() {{
    		put(TenantHandler.TENANT, tenantId);
    		put(TenantHandler.DPID, dpid);
    		put(TenantHandler.PORT, port);
    		put(TenantHandler.MAC, mac);
    	}};
    	
    	return ch.process(request);
    }

	public JSONRPC2Response createLink(final int tenantId, final String pathString) {
		CreateOVXLink cl = new CreateOVXLink();
		
    	@SuppressWarnings("serial")
		HashMap<String, Object> request = new HashMap<String, Object>() {{
    		put(TenantHandler.TENANT, tenantId);
    		put(TenantHandler.PATH, pathString);
		}};
		
		return cl.process(request);
	}
    
    
}
