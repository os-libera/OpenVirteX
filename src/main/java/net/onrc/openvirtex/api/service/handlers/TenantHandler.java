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
package net.onrc.openvirtex.api.service.handlers;

import java.util.HashMap;

import net.onrc.openvirtex.api.service.handlers.tenant.AddController;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectOVXLink;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.CreateOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectOVXRoute;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectHost;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectOVXLink;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.RemoveOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.DisconnectOVXRoute;
import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXBigSwitchRouting;
import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXLinkPath;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.StartOVXSwitch;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXNetwork;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXPort;
import net.onrc.openvirtex.api.service.handlers.tenant.StopOVXSwitch;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

/**
 * Creates handlers for all tenant API calls, and selects the appropriate
 * handler when processing a request.
 */
public class TenantHandler extends AbstractHandler implements RequestHandler {

    /**
     * Keyword for controller URLs.
     */
    public static final String CTRLURLS = "controllerUrls";
    /**
     * Keyword for the virtual network address space.
     */
    public static final String NETADD = "networkAddress";
    /**
     * Keyword for the virtual network mask.
     */
    public static final String NETMASK = "mask";
    /**
     * Keyword for the virtual network ID.
     */
    public static final String TENANT = "tenantId";
    /**
     * Keyword for the list of switches.
     */
    public static final String DPIDS = "dpids";
    /**
     * Keyword for the switch.
     */
    public static final String DPID = "dpid";
    /**
     * Keyword for the source switch.
     */
    public static final String SRC_DPID = "srcDpid";
    /**
     * Keyword for the destination switch.
     */
    public static final String DST_DPID = "dstDpid";
    /**
     * Keyword for the link ID.
     */
    public static final String LINK = "linkId";
    /**
     * Keyword for the switch route between ports.
     */
    public static final String SWITCH_ROUTE = "switch_route";
    /**
     * Keyword for the switch port.
     */
    public static final String PORT = "port";
    /**
     * Keyword for the virtual switch port.
     */
    public static final String VPORT = "vport";
    /**
     * Keyword for the virtual switch.
     */
    public static final String VDPID = "vdpid";
    /**
     * Keyword for the source switch port.
     */
    public static final String SRC_PORT = "srcPort";
    /**
     * Keyword for the destination switch port.
     */
    public static final String DST_PORT = "dstPort";
    /**
     * Keyword for the route priority.
     */
    public static final String PRIORITY = "priority";
    /**
     * Keyword for the MAC address.
     */
    public static final String MAC = "mac";
    /**
     * Keyword for the path.
     */
    public static final String PATH = "path";
    /**
     * Keyword for the routing algorithm.
     */
    public static final String ALGORITHM = "algorithm";
    /**
     * Keyword for the number of backup routes.
     */
    public static final String BACKUPS = "backup_num";
    /**
     * Keyword for the host ID.
     */
    public static final String HOST = "hostId";
    /**
     * Keyword for the route ID.
     */
    public static final String ROUTE = "routeId";
    /**
     * Keyword for the boot state.
     */
    public static final String IS_BOOTED = "isBooted";

    @SuppressWarnings({ "serial", "rawtypes" })
    private HashMap<String, ApiHandler> handlers = new HashMap<String, ApiHandler>() {
        {
            this.put("addControllers", new AddController());

            this.put("createNetwork", new CreateOVXNetwork());
            this.put("createSwitch", new CreateOVXSwitch());
            this.put("createPort", new CreateOVXPort());
            this.put("setInternalRouting", new SetOVXBigSwitchRouting());
            this.put("connectHost", new ConnectHost());
            this.put("connectLink", new ConnectOVXLink());
            this.put("setLinkPath", new SetOVXLinkPath());
            this.put("connectRoute", new ConnectOVXRoute());

            this.put("removeNetwork", new RemoveOVXNetwork());
            this.put("removeSwitch", new RemoveOVXSwitch());
            this.put("removePort", new RemoveOVXPort());
            this.put("disconnectHost", new DisconnectHost());
            this.put("disconnectLink", new DisconnectOVXLink());
            this.put("disconnectRoute", new DisconnectOVXRoute());

            this.put("startNetwork", new StartOVXNetwork());
            this.put("startSwitch", new StartOVXSwitch());
            this.put("startPort", new StartOVXPort());
            this.put("stopNetwork", new StopOVXNetwork());
            this.put("stopSwitch", new StopOVXSwitch());
            this.put("stopPort", new StopOVXPort());
        }
    };


    @Override
    public String[] handledRequests() {
        return this.handlers.keySet().toArray(new String[] {});
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JSONRPC2Response process(final JSONRPC2Request req,
            final MessageContext ctxt) {

        final ApiHandler m = this.handlers.get(req.getMethod());
        if (m != null) {

            if (m.getType() != JSONRPC2ParamsType.NO_PARAMS
                    && m.getType() != req.getParamsType()) {
                return new JSONRPC2Response(new JSONRPC2Error(
                        JSONRPC2Error.INVALID_PARAMS.getCode(), req.getMethod()
                                + " requires: " + m.getType() + "; got: "
                                + req.getParamsType()), req.getID());
            }

            switch (m.getType()) {
            case NO_PARAMS:
                return m.process(null);
            case ARRAY:
                return m.process(req.getPositionalParams());
            case OBJECT:
                return m.process(req.getNamedParams());
            default:
                break;
            }
        }

        return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
    }
}
