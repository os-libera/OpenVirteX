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

import net.onrc.openvirtex.core.io.SwitchChannelPipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class OpenVirteXController implements Runnable {

	Logger log = LogManager.getLogger(OpenVirteXController.class.getName());

	private static final int SEND_BUFFER_SIZE = 1024 * 1024;

	private String configFile = null;
	private String ofHost = null;
	private Integer ofPort = null;

	private final ChannelGroup cg = new DefaultChannelGroup();
	private SwitchChannelPipeline pfact = null;

	public OpenVirteXController(String configFile, String ofHost, Integer ofPort) {
		this.configFile = configFile;
		this.ofHost = ofHost;
		this.ofPort = ofPort;
	}

	@Override
	public void run() {
		Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
		try {
			final ServerBootstrap switchServerBootStrap = createServerBootStrap();

			setServerBootStrapParams(switchServerBootStrap);

			pfact = new SwitchChannelPipeline(this, null);
			switchServerBootStrap.setPipelineFactory(pfact);
			InetSocketAddress sa = (ofHost == null) ? new InetSocketAddress(
					ofPort) : new InetSocketAddress(ofHost, ofPort);

			cg.add(switchServerBootStrap.bind(sa));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void setServerBootStrapParams(ServerBootstrap bootstrap) {
		bootstrap.setOption("reuseAddr", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.sendBufferSize",
				OpenVirteXController.SEND_BUFFER_SIZE);

	}

	private ServerBootstrap createServerBootStrap() {
		return new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
	}

	public void terminate() {
		if (cg != null && cg.close().awaitUninterruptibly(1000)) {
			log.info("Shut down all connections. Quitting...");
		} else {
			log.error("Error shutting down all connections. Quitting anyway.");
		}
		if (pfact != null)
			pfact.releaseExternalResources();
	}

}
