/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.core;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import net.onrc.openvirtex.api.server.JettyServer;
import net.onrc.openvirtex.core.io.ClientChannelPipeline;
import net.onrc.openvirtex.core.io.SwitchChannelPipeline;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLinkField;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

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
	private static BitSetIndex tenantIdCounter = null;
	
	private String configFile = null;
	private String ofHost = null;
	private Integer ofPort = null;
	private String dbHost = null;
	private Integer dbPort = null;
	private Boolean dbClear = null;
	Thread server;

	private final NioClientSocketChannelFactory clientSockets = new NioClientSocketChannelFactory(
			Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

	private final ChannelGroup sg = new DefaultChannelGroup();
	private final ChannelGroup cg = new DefaultChannelGroup();

	private SwitchChannelPipeline pfact = null;
	private ClientChannelPipeline cfact = null;

	private int maxVirtual = 0;
	private OVXLinkField	ovxLinkField;

	private Integer statsRefresh = 30;

	public OpenVirteXController(final String configFile, final String ofHost,
			final Integer ofPort, final int maxVirtual, final String dbHost, final int dbPort,
			final Boolean dbClear, Integer statsRefresh) {
		this.configFile = configFile;
		this.ofHost = ofHost;
		this.ofPort = ofPort;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
		this.dbClear = dbClear;
		this.maxVirtual = maxVirtual;
		//by default, use Mac addresses to store vLinks informations
		this.ovxLinkField = OVXLinkField.MAC_ADDRESS;
		this.statsRefresh  = statsRefresh;
		OpenVirteXController.instance = this;
		OpenVirteXController.tenantIdCounter = new BitSetIndex(IndexType.TENANT_ID);
	}

	@Override
	public void run() {
		Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
		PhysicalNetwork.getInstance().boot();

		this.startDatabase();
		this.startServer();

		try {
			final ServerBootstrap switchServerBootStrap = this
					.createServerBootStrap();

			this.setServerBootStrapParams(switchServerBootStrap);

			this.pfact = new SwitchChannelPipeline(this, null);
			switchServerBootStrap.setPipelineFactory(this.pfact);
			final InetSocketAddress sa = this.ofHost == null ? new InetSocketAddress(
					this.ofPort) : new InetSocketAddress(this.ofHost,
							this.ofPort);
					this.sg.add(switchServerBootStrap.bind(sa));

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void registerOVXSwitch(final OVXSwitch sw) {
		OVXNetwork ovxNetwork;
		try {
			ovxNetwork = sw.getMap().getVirtualNetwork(sw.getTenantId());
		} catch (NetworkMappingException e) {
			OpenVirteXController.this.log.error(
					"Could not connect to controller for switch: " + e.getMessage());
			return;
		}
		final String host = ovxNetwork.getControllerHost();
		final Integer port = ovxNetwork.getControllerPort();

		final ClientBootstrap clientBootStrap = this.createClientBootStrap();
		this.setClientBootStrapParams(clientBootStrap);
		final InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
		clientBootStrap.setOption("remoteAddress", remoteAddr);

		this.cfact = new ClientChannelPipeline(this, this.cg, null,
				clientBootStrap, sw);
		clientBootStrap.setPipelineFactory(this.cfact);

		final ChannelFuture cf = clientBootStrap.connect();

		cf.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture e)
					throws Exception {
				if (e.isSuccess()) {
					final Channel chan = e.getChannel();
					sw.setChannel(chan);
					OpenVirteXController.this.cg.add(chan);
				} else {
					OpenVirteXController.this.log.error(
							"Failed to connect to controller {} for switch {}",
							remoteAddr, sw.getSwitchName());
				}
			}
		});
	}

	private void setServerBootStrapParams(final ServerBootstrap bootstrap) {
		bootstrap.setOption("reuseAddr", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.sendBufferSize",
				OpenVirteXController.SEND_BUFFER_SIZE);

	}

	private void setClientBootStrapParams(final ClientBootstrap bootstrap) {
		bootstrap.setOption("reuseAddr", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.sendBufferSize",
				OpenVirteXController.SEND_BUFFER_SIZE);

	}

	private ClientBootstrap createClientBootStrap() {
		return new ClientBootstrap(this.clientSockets);
	}

	private ServerBootstrap createServerBootStrap() {
		return new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
	}

	private void startDatabase() {
		DBManager dbManager = DBManager.getInstance();
		dbManager.init(this.dbHost, this.dbPort, this.dbClear);
	}

	private void startServer() {
		// TODO: pass this via cmd args.
		this.server = new Thread(new JettyServer(8080));
		this.server.start();
	}

	public void terminate() {
		if (this.cg != null && this.cg.close().awaitUninterruptibly(1000)) {
			this.log.info("Shut down all controller connections. Quitting...");
		} else {
			this.log.error("Error shutting down all controller connections. Quitting anyway.");
		}

		if (this.sg != null && this.sg.close().awaitUninterruptibly(1000)) {
			this.log.info("Shut down all switch connections. Quitting...");
		} else {
			this.log.error("Error shutting down all switch connections. Quitting anyway.");
		}

		if (this.pfact != null) {
			this.pfact.releaseExternalResources();
		}
		if (this.cfact != null) {
			this.cfact.releaseExternalResources();
		}

		this.log.info("Shutting down database connection");
		DBManager.getInstance().close();
	}

	public static OpenVirteXController getInstance() {
		if (OpenVirteXController.instance == null) {
			throw new RuntimeException(
					"The OpenVirtexController has not been initialized; quitting.");
		}
		return OpenVirteXController.instance;
	}

	/*
	 * return the number of bits needed to encode the tenant id
	 */
	public int getNumberVirtualNets() {
		return this.maxVirtual;
	}

	public OVXLinkField getOvxLinkField() {
		return this.ovxLinkField;
	}

	public Integer getStatsRefresh() {
		return this.statsRefresh;
	}

	public static BitSetIndex getTenantCounter() {
		if (OpenVirteXController.instance == null) {
			throw new RuntimeException(
					"The OpenVirtexController has not been initialized; quitting.");
		}
		return tenantIdCounter;
	}
	
}
