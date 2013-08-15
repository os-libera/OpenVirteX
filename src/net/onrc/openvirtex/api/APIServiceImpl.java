package net.onrc.openvirtex.api;

import java.util.List;

import org.apache.thrift.TException;

public class APIServiceImpl implements TenantServer.Iface {

    APITenantManager tenantManager;

    public APIServiceImpl() {
	this.tenantManager = new APITenantManager();
    }

    @Override
    public int createVirtualNetwork(final String protocol,
	    final String controllerAddress, final short controllerPort,
	    final String networkAddress, final short mask) throws TException {
	return this.tenantManager.createOVXNetwork(protocol, controllerAddress,
	        controllerPort, networkAddress, mask);
    }

    @Override
    public long createVirtualSwitch(final int tenantId, final List<String> dpids)
	    throws TException {
	return this.tenantManager.createOVXSwitch(tenantId, dpids);
    }

    @Override
    public int createHost(final int tenantId, final String dpid,
	    final short portNumber, final String mac) throws TException {
	return this.tenantManager.createEdgePort(tenantId, Long.valueOf(dpid),
	        portNumber, mac);
    }

    @Override
    public int createVirtualLink(final int tenantId, final String pathString)
	    throws TException {
	return this.tenantManager.createOVXLink(tenantId, pathString);
    }

    @Override
    public boolean startNetwork(final int tenantId) throws TException {
	return this.tenantManager.bootNetwork(tenantId);
    }

    @Override
    public String saveConfig() throws TException {
	System.out.println(this.tenantManager.saveConfig());
	return this.tenantManager.saveConfig();
    }

}
