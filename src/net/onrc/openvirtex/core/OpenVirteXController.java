/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.core;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import net.onrc.openvirtex.core.io.ClientChannelPipeline;
import net.onrc.openvirtex.core.io.SwitchChannelPipeline;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


public class OpenVirteXController implements Runnable {

    Logger log = LogManager.getLogger(OpenVirteXController.class.getName());

    private static final int SEND_BUFFER_SIZE = 1024 * 1024;
    private static OpenVirteXController instance = null;
    
    
    private String configFile = null;
    private String ofHost = null;
    private Integer ofPort = null;


    private NioClientSocketChannelFactory clientSockets = new NioClientSocketChannelFactory(
	    Executors.newCachedThreadPool(), 
	    Executors.newCachedThreadPool());

    private final ChannelGroup sg = new DefaultChannelGroup();
    private final ChannelGroup cg = new DefaultChannelGroup();
    
    private SwitchChannelPipeline pfact = null;
    private ClientChannelPipeline cfact = null;

    private int maxVirtual = 0;

    public OpenVirteXController(String configFile, String ofHost, Integer ofPort, int maxVirtual) {
	this.configFile = configFile;
	this.ofHost = ofHost;
	this.ofPort = ofPort;
	this.maxVirtual  = maxVirtual;
	instance = this;
    }

    @Override
    public void run() {
	Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
	// Ensure PhysicalNetwork is instantiated
	PhysicalNetwork.getInstance();
//	OVXSingleSwitch sw = new OVXSingleSwitch(1,1);
//	sw.init();
//	this.registerOVXSwitch(sw, "192.168.2.136", 6633);
	//this.registerOVXSwitch(new OVXSingleSwitch("fake", (long)2, null, 1, (short)100), "192.168.2.136", 6633);
	try {
	    final ServerBootstrap switchServerBootStrap = createServerBootStrap();

	    setServerBootStrapParams(switchServerBootStrap);

	    pfact = new SwitchChannelPipeline(this, null);
	    switchServerBootStrap.setPipelineFactory(pfact);
	    InetSocketAddress sa = (ofHost == null) ? new InetSocketAddress(
		    ofPort) : new InetSocketAddress(ofHost, ofPort);
		    sg.add(switchServerBootStrap.bind(sa));

	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

    }

    public void registerOVXSwitch(final OVXSwitch sw, String host, Integer port) {
	ClientBootstrap clientBootStrap = createClientBootStrap();
	setClientBootStrapParams(clientBootStrap);
	final InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
	clientBootStrap.setOption("remoteAddress", remoteAddr);

	cfact = new ClientChannelPipeline(this, cg, null,
		clientBootStrap, sw);
	clientBootStrap.setPipelineFactory(cfact);

	ChannelFuture cf = clientBootStrap.connect();

	cf.addListener(new ChannelFutureListener() {

	    @Override
	    public void operationComplete(ChannelFuture e) throws Exception {
		if (e.isSuccess()) {
		    Channel chan = e.getChannel();
		    sw.setChannel(chan);
		    cg.add(chan);
		} else
		    log.error("Failed to connect to controller {} for switch {}", remoteAddr, sw.getSwitchId());

	    }
	});

    }


    private void setServerBootStrapParams(ServerBootstrap bootstrap) {
	bootstrap.setOption("reuseAddr", true);
	bootstrap.setOption("child.keepAlive", true);
	bootstrap.setOption("child.tcpNoDelay", true);
	bootstrap.setOption("child.sendBufferSize",
		OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private void setClientBootStrapParams(ClientBootstrap bootstrap) {
	bootstrap.setOption("reuseAddr", true);
	bootstrap.setOption("child.keepAlive", true);
	bootstrap.setOption("child.tcpNoDelay", true);
	bootstrap.setOption("child.sendBufferSize",
		OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private ClientBootstrap createClientBootStrap() {
	return new ClientBootstrap(clientSockets);
    }

    private ServerBootstrap createServerBootStrap() {
	return new ServerBootstrap(new NioServerSocketChannelFactory(
		Executors.newCachedThreadPool(),
		Executors.newCachedThreadPool()));
    }

    public void terminate() {
	if (cg != null && cg.close().awaitUninterruptibly(1000)) {
	    log.info("Shut down all controller connections. Quitting...");
	} else {
	    log.error("Error shutting down all controller connections. Quitting anyway.");
	}
	
	if (sg != null && sg.close().awaitUninterruptibly(1000)) {
	    log.info("Shut down all switch connections. Quitting...");
	} else {
	    log.error("Error shutting down all switch connections. Quitting anyway.");
	}
	
	if (pfact != null)
	    pfact.releaseExternalResources();
	if (cfact != null)
	    cfact.releaseExternalResources();
    }
    
    public static OpenVirteXController getInstance() {
	if (instance == null)
	    throw new RuntimeException("The OpenVirtexController has not been initialized; quitting.");
	return instance;
    }
    
    /*
     * return the number of bits needed to encode the tenant id
     */
    public int getNumberVirtualNets() {
	return this.maxVirtual;
    }

}
