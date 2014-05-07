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
package net.onrc.openvirtex.api.server;

import java.io.File;

import net.onrc.openvirtex.api.JSONRPCAPI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Run a JSON RPC web server that supports both http and https. Creates three
 * roles (user, admin, and ui) each with an exposed resource (/tenant, /admin,
 * and /status).
 *
 */
public class JettyServer implements Runnable {

    private static Logger log = LogManager.getLogger(JettyServer.class
            .getName());

    /**
     * Web server realm name.
     */
    public static final String REALM = "OVXREALM";

    private JSONRPCAPI service = null;
    private Server server = null;

    /**
     * Constructs and initializes a web server.
     *
     * @param port
     *            the port on which to run the web server
     */
    public JettyServer(final int port) {
        this.service = new JSONRPCAPI();
        this.init(port);
    }

    /**
     * Initializes API web server.
     *
     * @param port
     *            the port on which to run the web server
     */
    private void init(final int port) {
        JettyServer.log.info("Initializing API WebServer on port {}", port);
        this.server = new Server(port);

        final String sslKeyStore = System.getProperty("javax.net.ssl.keyStore");

        if (sslKeyStore == null) {
            throw new RuntimeException(
                    "Property javax.net.ssl.keyStore not defined; missing keystore file:"
                            + "Use startup script to start OVX");
        }
        if (!new File(sslKeyStore).exists()) {
            throw new RuntimeException(
                    "SSL Key Store file not found: '"
                            + sslKeyStore
                            + " make sure you installed OVX correctly : see Installation manual");
        }

        // HTTP Configuration
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);

        // HTTP connector
        final ServerConnector http = new ServerConnector(this.server,
                new HttpConnectionFactory(httpConfig));
        http.setPort(port);
        http.setIdleTimeout(30000);

        // SSL Context Factory for HTTPS and SPDY
        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(sslKeyStore);
        sslContextFactory
                .setKeyStorePassword("OBF:1lbw1wg41sox1kfx1vub1w8t1idn1zer1zej1igj1w8x1vuz1kch1sot1wfu1lfm");
        sslContextFactory
                .setKeyManagerPassword("OBF:1ym71u2g1uh61l8h1l4t1ugk1u2u1ym7");

        // HTTPS Configuration
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // HTTPS connector
        final ServerConnector https = new ServerConnector(this.server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConfig));
        https.setPort(8443);
        https.setIdleTimeout(500000);

        // Set the connectors
        this.server.setConnectors(new Connector[] {http, https});

        final Constraint userConstraint = new Constraint();
        userConstraint.setName(Constraint.__BASIC_AUTH);
        userConstraint.setRoles(new String[] {"user"});
        userConstraint.setAuthenticate(true);

        final Constraint adminConstraint = new Constraint();
        adminConstraint.setName(Constraint.__BASIC_AUTH);
        adminConstraint.setRoles(new String[] {"admin"});
        adminConstraint.setAuthenticate(true);

        final Constraint uiConstraint = new Constraint();
        uiConstraint.setName(Constraint.__BASIC_AUTH);
        uiConstraint.setRoles(new String[] {"ui"});
        uiConstraint.setAuthenticate(true);

        final ConstraintMapping usermapping = new ConstraintMapping();
        usermapping.setConstraint(userConstraint);
        usermapping.setPathSpec("/tenant");

        final ConstraintMapping adminmapping = new ConstraintMapping();
        adminmapping.setConstraint(adminConstraint);
        adminmapping.setPathSpec("/admin");

        final ConstraintMapping uimapping = new ConstraintMapping();
        uimapping.setConstraint(uiConstraint);
        uimapping.setPathSpec("/status");

        final ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setRealmName(JettyServer.REALM);
        sh.setConstraintMappings(new ConstraintMapping[] {usermapping,
                adminmapping, uimapping});
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setHandler(this.service);
        final LoginService loginSrv = new OVXLoginService();
        sh.setLoginService(loginSrv);

        this.server.setHandler(sh);
    }

    @Override
    public void run() {
        try {
            this.server.start();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            this.server.join();
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

    }

}
