package net.onrc.openvirtex.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.onrc.openvirtex.api.service.AdminService;
import net.onrc.openvirtex.api.service.TenantService;
import net.onrc.openvirtex.api.service.UIService;

import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JSONRPCAPI extends AbstractHandler {

	private final UIService uiService;
	private final TenantService tenantService;
	private final AdminService adminService;

	public JSONRPCAPI() {
		this.tenantService = new TenantService();
		this.uiService = new UIService();
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
		if (target.equals("/ui")) {
			this.uiService.handle(request, response);

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
