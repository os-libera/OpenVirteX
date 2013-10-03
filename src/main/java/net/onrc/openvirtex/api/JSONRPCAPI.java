/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
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

public class JSONRPCAPI extends AbstractHandler {

	private final MonitoringService monitoringService;
	private final TenantService tenantService;
	private final AdminService adminService;

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
