package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AbstractAPICalls extends TestCase {

	public JSONRPC2Response createNetwork() {
		return this.createNetwork(6633);
	}

	public JSONRPC2Response createNetwork(final Integer port) {
		final CreateOVXNetwork cn = new CreateOVXNetwork();

		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(TenantHandler.PROTOCOL, "tcp");
				this.put(TenantHandler.CTRLHOST, "localhost");
				this.put(TenantHandler.CTRLPORT, port);
				this.put(TenantHandler.NETADD, "10.0.0.0");
				this.put(TenantHandler.NETMASK, 24);
			}
		};

		return cn.process(request);

	}

	public JSONRPC2Response createSwitch(final Integer tenantId,
			final List<String> dpids) {
		OVXMap.getInstance();
		final CreateOVXSwitch cs = new CreateOVXSwitch();

		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(TenantHandler.TENANT, tenantId);
				this.put(TenantHandler.DPIDS, dpids);
			}
		};

		return cs.process(request);

	}

	public JSONRPC2Response connectHost(final Integer tenantId,
			final Long dpid, final Short port, final String mac) {

		final ConnectHost ch = new ConnectHost();

		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(TenantHandler.TENANT, tenantId);
				this.put(TenantHandler.DPID, dpid);
				this.put(TenantHandler.PORT, port);
				this.put(TenantHandler.MAC, mac);
			}
		};

		return ch.process(request);
	}

	public JSONRPC2Response createLink(final int tenantId,
			final String pathString) {
		final CreateOVXLink cl = new CreateOVXLink();

		@SuppressWarnings("serial")
		final HashMap<String, Object> request = new HashMap<String, Object>() {
			{
				this.put(TenantHandler.TENANT, tenantId);
				this.put(TenantHandler.PATH, pathString);
			}
		};

		return cl.process(request);
	}

}
