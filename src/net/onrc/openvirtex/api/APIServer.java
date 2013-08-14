package net.onrc.openvirtex.api;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

public class APIServer implements Runnable{
    APITenantManager tenantManager;
    TServer server;

    public APIServer() {
	this.tenantManager = new APITenantManager();
    }

    private void start() {
	try {
	    final int port = 8080;
	    final TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
		    port);
	    final TenantServer.Processor<APIServiceImpl> processor = new TenantServer.Processor<APIServiceImpl>(
		    new APIServiceImpl());
	    this.server = new TNonblockingServer(
		    new TNonblockingServer.Args(serverTransport)
		            .processor(processor));
	    this.server.serve();
	    
	} catch (final TTransportException e) {
	    e.printStackTrace();
	}
    }

    public static void main(final String[] agrs) {
	new Thread(new APIServer()).start();
    }

    @Override
    public void run() {
	this.start();
    }
    
    public void stop() {
	this.server.stop();
    }
    
   
}
