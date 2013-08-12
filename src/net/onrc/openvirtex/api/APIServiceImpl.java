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
	// TODO Auto-generated method stub
	return this.tenantManager.createOVXNetwork(protocol, controllerAddress,
	        controllerPort, networkAddress, mask);
    }

    @Override
    public long createVirtualSwitch(final int tenantId, final List<String> dpids)
	    throws TException {
	// TODO Auto-generated method stub
	return this.tenantManager.createOVXSwitch(tenantId, dpids);
    }

    @Override
    public int createHost(final int tenantId, final String dpid,
	    final short portNumber) throws TException {
	// TODO Auto-generated method stub
	final String mac = "";
	return this.tenantManager.createEdgePort(tenantId, mac);
    }

    @Override
    public int createVirtualLink(final int tenantId, final String pathString)
	    throws TException {
	// TODO Auto-generated method stub
	return this.tenantManager.createOVXLink(tenantId, pathString);
    }

    @Override
    public boolean startNetwork(final int tenantId) throws TException {
	// TODO Auto-generated method stub
	return this.tenantManager.bootNetwork(tenantId);
    }

}
