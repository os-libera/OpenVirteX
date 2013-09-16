package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.HashMap;
import java.util.List;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import junit.framework.TestCase;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.Mappable;
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
    
    
    
    
    public void connectHost() {
    	HashMap<String, Object> request = new HashMap<>();
    	
    }
    
    
}
