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
	
	private UIService uiService;
	private TenantService tenantService;
	private AdminService adminService;

	public JSONRPCAPI(){
		tenantService = new TenantService();
		uiService = new UIService();
		adminService = new AdminService();
	}

	@Override
	public void handle(String target,Request baseRequest,
			HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException {
		
		
		
		if(baseRequest.getAuthentication() == null ||
				baseRequest.getAuthentication().equals(Authentication.UNAUTHENTICATED)){
			response.sendError(Response.SC_UNAUTHORIZED, "Permission denied.");
			baseRequest.setHandled(true);
			
			return;
		}
		if (target.equals("/ui")) {
			uiService.handle(request, response);
		
		} else if (target.equals("/tenant")) { 	
			tenantService.handle(request, response);
		
		} else if (target.equals("/admin")) {
			adminService.handle(request, response);
		
		} else {
			response.sendError(Response.SC_NOT_FOUND, target + " is not a service offered by OVX");
		
		}
		baseRequest.setHandled(true);
	}

}
