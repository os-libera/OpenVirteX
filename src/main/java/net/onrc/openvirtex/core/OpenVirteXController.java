/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.core;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.api.server.JettyServer;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.core.io.ClientChannelPipeline;
import net.onrc.openvirtex.core.io.SwitchChannelPipeline;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLinkField;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
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
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.openflow.vendor.nicira.OFNiciraVendorExtensions;

public class OpenVirteXController implements Runnable {

    Logger log = LogManager.getLogger(OpenVirteXController.class.getName());

    private static final int SEND_BUFFER_SIZE = 1024 * 1024;
    private static OpenVirteXController instance = null;
    private static BitSetIndex tenantIdCounter = null;

    @SuppressWarnings("unused")
    private String configFile = null;
    private String ofHost = null;
    private Integer ofPort = null;
    private String dbHost = null;
    private Integer dbPort = null;
    private Boolean dbClear = null;
    Thread server;

    private final NioClientSocketChannelFactory clientSockets = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

    private ThreadPoolExecutor clientThreads = null;
    private ThreadPoolExecutor serverThreads = null;

    private final ChannelGroup sg = new DefaultChannelGroup();
    private final ChannelGroup cg = new DefaultChannelGroup();

    private SwitchChannelPipeline pfact = null;
    private ClientChannelPipeline cfact = null;

    private int maxVirtual = 0;
    private OVXLinkField ovxLinkField;

    private Integer statsRefresh;

    private Integer nClientThreads;

    private Integer nServerThreads;

    private final Boolean useBDDP;

    public OpenVirteXController(CmdLineSettings settings) {
        this.ofHost = settings.getOFHost();
        this.ofPort = settings.getOFPort();
        this.dbHost = settings.getDBHost();
        this.dbPort = settings.getDBPort();
        this.dbClear = settings.getDBClear();
        this.maxVirtual = settings.getNumberOfVirtualNets();
        this.statsRefresh = settings.getStatsRefresh();
        this.nClientThreads = settings.getClientThreads();
        this.nServerThreads = settings.getServerThreads();
        this.useBDDP = settings.getUseBDDP();
        // by default, use Mac addresses to store vLinks informations
        this.ovxLinkField = OVXLinkField.MAC_ADDRESS;
        this.clientThreads = new OrderedMemoryAwareThreadPoolExecutor(
                nClientThreads, 1048576, 1048576, 5, TimeUnit.SECONDS);
        this.serverThreads = new OrderedMemoryAwareThreadPoolExecutor(
                nServerThreads, 1048576, 1048576, 5, TimeUnit.SECONDS);
        this.pfact = new SwitchChannelPipeline(this, this.serverThreads);
        OpenVirteXController.instance = this;
        OpenVirteXController.tenantIdCounter = new BitSetIndex(
                IndexType.TENANT_ID);
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
        initVendorMessages();
        PhysicalNetwork.getInstance().boot();

        this.startDatabase();
        this.startServer();

        try {
            final ServerBootstrap switchServerBootStrap = this
                    .createServerBootStrap();

            this.setServerBootStrapParams(switchServerBootStrap);

            switchServerBootStrap.setPipelineFactory(this.pfact);
            final InetSocketAddress sa = this.ofHost == null ? new InetSocketAddress(
                    this.ofPort) : new InetSocketAddress(this.ofHost,
                    this.ofPort);
            this.sg.add(switchServerBootStrap.bind(sa));

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void addControllers(final OVXSwitch sw, final Set<String> ctrls) {
        String[] ctrlParts = null;
        for (String ctrl : ctrls) {
            ctrlParts = ctrl.split(":");
            final ClientBootstrap clientBootStrap = this
                    .createClientBootStrap();
            this.setClientBootStrapParams(clientBootStrap);
            final InetSocketAddress remoteAddr = new InetSocketAddress(
                    ctrlParts[1], Integer.parseInt(ctrlParts[2]));
            clientBootStrap.setOption("remoteAddress", remoteAddr);

            this.cfact = new ClientChannelPipeline(this, this.cg,
                    this.clientThreads, clientBootStrap, sw);
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
                        OpenVirteXController.this.log
                                .error("Failed to connect to controller {} for switch {}",
                                        remoteAddr, sw.getSwitchName());
                    }
                }
            });
        }
    }

    public void registerOVXSwitch(final OVXSwitch sw) {
        OVXNetwork ovxNetwork;
        try {
            ovxNetwork = sw.getMap().getVirtualNetwork(sw.getTenantId());
        } catch (NetworkMappingException e) {
            OpenVirteXController.this.log
                    .error("Could not connect to controller for switch: "
                            + e.getMessage());
            return;
        }

        final Set<String> ctrls = ovxNetwork.getControllerUrls();
        addControllers(sw, ctrls);
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

    private void initVendorMessages() {
        // Configure openflowj to be able to parse the role request/reply
        // vendor messages.
        OFNiciraVendorExtensions.initialize();

    }

    public Boolean getUseBDDP() {
        return this.useBDDP;
    }

}
