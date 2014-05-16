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
package net.onrc.openvirtex.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.onrc.openvirtex.api.service.AdminService;
import net.onrc.openvirtex.api.service.MonitoringService;
import net.onrc.openvirtex.api.service.TenantService;

import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * This class manages JSON RPC API services. It creates the monitoring, tenant
 * and admin services. It implements the main handler for incoming requests and
 * redirects them to the appropriate service.
 *
 */
public class JSONRPCAPI extends AbstractHandler {

    private final MonitoringService monitoringService;
    private final TenantService tenantService;
    private final AdminService adminService;

    /**
     * Constructor for JSON RPC handler. Creates tenant, monitoring and admin
     * services.
     */
    public JSONRPCAPI() {
        this.tenantService = new TenantService();
        this.monitoringService = new MonitoringService();
        this.adminService = new AdminService();
    }

    @Override
    public void handle(final String target, final Request baseRequest,
            final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        if (baseRequest.getAuthentication() == null
                || baseRequest.getAuthentication().equals(
                        Authentication.UNAUTHENTICATED)) {
            response.sendError(Response.SC_UNAUTHORIZED, "Permission denied.");
            baseRequest.setHandled(true);

            return;
        }
        if (target.equals("/status")) {
            this.monitoringService.handle(request, response);

        } else if (target.equals("/tenant")) {
            this.tenantService.handle(request, response);

        } else if (target.equals("/admin")) {
            this.adminService.handle(request, response);

        } else {
            response.sendError(Response.SC_NOT_FOUND, target
                    + " is not a service offered by OVX");

        }
        baseRequest.setHandled(true);
    }

}
