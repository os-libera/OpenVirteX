/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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
			final List<Integer> dpids) {
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
	
	public JSONRPC2Response createSwitchRoute(final int tenantId, final long dpid,
		final short srcPort, final short dstPort,
		final String pathString) {
	    final CreateOVXSwitchRoute cr = new CreateOVXSwitchRoute();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		    this.put(TenantHandler.DPID, dpid);
		    this.put(TenantHandler.SRC_PORT, srcPort);
		    this.put(TenantHandler.DST_PORT, dstPort);
		    this.put(TenantHandler.PATH, pathString);
		}
	    };

	    return cr.process(request);
	}

	public JSONRPC2Response removeNetwork(final int tenantId) {
	    final RemoveOVXNetwork rn = new RemoveOVXNetwork();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		}
	    };

	    return rn.process(request);
	}
	
	public JSONRPC2Response removeSwitch(final int tenantId, final long dpid) {
	    final RemoveOVXSwitch rs = new RemoveOVXSwitch();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		    this.put(TenantHandler.DPID, dpid);
		}
	    };

	    return rs.process(request);
	}
	
	public JSONRPC2Response removeLink(final int tenantId, final int linkId) {
	    final RemoveOVXLink cl = new RemoveOVXLink();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		    this.put(TenantHandler.LINK, linkId);
		}
	    };

	    return cl.process(request);
	}
	
	public JSONRPC2Response disconnectHost(final Integer tenantId,
		final Long dpid, final Short port, final String mac) {

	    final DisconnectHost ch = new DisconnectHost();

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
	
	public JSONRPC2Response removeSwitchRoute(final int tenantId, final long dpid,
		final short srcPort, final short dstPort) {
	    final RemoveOVXSwitchRoute cl = new RemoveOVXSwitchRoute();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		    this.put(TenantHandler.DPID, dpid);
		    this.put(TenantHandler.SRC_PORT, srcPort);
		    this.put(TenantHandler.DST_PORT, dstPort);
		}
	    };

	    return cl.process(request);
	}
	
	public JSONRPC2Response stopNetwork(final int tenantId) {
	    final StopNetwork cl = new StopNetwork();

	    @SuppressWarnings("serial")
	    final HashMap<String, Object> request = new HashMap<String, Object>() {
		{
		    this.put(TenantHandler.TENANT, tenantId);
		}
	    };

	    return cl.process(request);
	}
}
