package net.onrc.openvirtex.api;

import net.onrc.openvirtex.core.OpenVirteXController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

public class APIServer implements Runnable {

    Logger           log = LogManager.getLogger(APIServer.class.getName());
    APITenantManager tenantManager;
    TServer          server;

    public APIServer() {
	this.tenantManager = APITenantManager.getInstance();
    }

    private void start() {
	try {
	    final int port = OpenVirteXController.getApiPort();
	    final TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
		    port);
	    final TenantServer.Processor<APIServiceImpl> processor = new TenantServer.Processor<APIServiceImpl>(
		    new APIServiceImpl());
	    this.server = new TNonblockingServer(new TNonblockingServer.Args(
		    serverTransport).processor(processor));
	    this.log.info("Starting API server");
	    this.server.serve();

	} catch (final TTransportException e) {
	    System.out.println("Inside exception");
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
