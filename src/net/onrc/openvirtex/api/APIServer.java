package net.onrc.openvirtex.api;


import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

public class APIServer {
    APITenantManager tenantManager;
    public APIServer () {
	tenantManager = new APITenantManager();
    }

    private void start() {
	try {
	    int port = 8080;
	    TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
	    TenantServer.Processor<APIServiceImpl> processor = new TenantServer.Processor<APIServiceImpl>(new APIServiceImpl());
	    TServer server = new TNonblockingServer(new TNonblockingServer.Args(serverTransport).processor(processor));
	    server.serve();
	} catch (TTransportException e) {
	    e.printStackTrace();
	}
    }

    public static void main (String[] agrs) {
	APIServer server = new APIServer();
	server.start();
    }
 }
