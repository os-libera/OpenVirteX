package net.onrc.openvirtex.api.server;

import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.UserIdentity.Scope;

public class OVXLoginService implements LoginService {

	private IdentityService identityService = new DefaultIdentityService();
	
	@Override
	public IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	public String getName() {
		return JettyServer.REALM_NAME;
	}

	@Override
	public UserIdentity login(String username, Object credentials) {
		return new OpenVirteXAuthenticatedUser(username, (String) credentials).getUserIdentity();
	}

	@Override
	public void logout(UserIdentity arg0) {}

	@Override
	public void setIdentityService(IdentityService arg0) {}

	@Override
	public boolean validate(UserIdentity arg0) {
		
		return false;
	}
	
	public class OpenVirteXAuthenticatedUser implements Authentication.User {


		private String user;
		private String password;

		public OpenVirteXAuthenticatedUser(String username, String password){
			this.user = username;
			this.password = password;
		}

		@Override
		public String getAuthMethod() {
			return "BASIC";
		}

		@Override
		public UserIdentity getUserIdentity() {
			//TODO: need to return the correct identity for this user.
			// Permitting specific logins for now with no passwords
			if (user.equals("tenant")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(user) , new String[] {"user"});
			} else if (user.equals("ui")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(user) , new String[] {"ui"});
			} else if (user.equals("admin")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(user) , new String[] {"user", "admin", "ui"});
			} else
				return null;
			
		}

		@Override
		public boolean isUserInRole(Scope scope, String role) {
			System.out.println("role " + role);
			return true;
		}

		@Override
		public void logout() {
			//TODO: remove any acquired tokens.

		}

	}


}
