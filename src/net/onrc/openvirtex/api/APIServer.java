package net.onrc.openvirtex.api;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

public class APIServer {
    APITenantManager tenantManager;

    public APIServer() {
	this.tenantManager = new APITenantManager();
    }

    private void start() {
	try {
	    final int port = 8000;
	    final TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
		    port);
	    final TenantServer.Processor<APIServiceImpl> processor = new TenantServer.Processor<APIServiceImpl>(
		    new APIServiceImpl());
	    final TServer server = new TNonblockingServer(
		    new TNonblockingServer.Args(serverTransport)
		            .processor(processor));
	    server.serve();
	} catch (final TTransportException e) {
	    e.printStackTrace();
	}
    }

    public static void main(final String[] agrs) {
	final APIServer server = new APIServer();
	server.start();
    }
}
