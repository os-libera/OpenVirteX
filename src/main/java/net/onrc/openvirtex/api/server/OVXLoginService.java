/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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

	private final IdentityService identityService = new DefaultIdentityService();

	@Override
	public IdentityService getIdentityService() {
		return this.identityService;
	}

	@Override
	public String getName() {
		return JettyServer.REALM_NAME;
	}

	@Override
	public UserIdentity login(final String username, final Object credentials) {
		return new OpenVirteXAuthenticatedUser(username, (String) credentials)
				.getUserIdentity();
	}

	@Override
	public void logout(final UserIdentity arg0) {
	}

	@Override
	public void setIdentityService(final IdentityService arg0) {
	}

	@Override
	public boolean validate(final UserIdentity arg0) {

		return false;
	}

	public class OpenVirteXAuthenticatedUser implements Authentication.User {

		private final String user;
		@SuppressWarnings("unused")
		private final String password;

		public OpenVirteXAuthenticatedUser(final String username,
				final String password) {
			this.user = username;
			this.password = password;
		}

		@Override
		public String getAuthMethod() {
			return "BASIC";
		}

		@Override
		public UserIdentity getUserIdentity() {
			// TODO: need to return the correct identity for this user.
			// Permitting specific logins for now with no passwords
			if (this.user.equals("tenant")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(
						this.user), new String[] { "user" });
			} else if (this.user.equals("ui")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(
						this.user), new String[] { "ui" });
			} else if (this.user.equals("admin")) {
				return new DefaultUserIdentity(new Subject(), new JMXPrincipal(
						this.user), new String[] { "user", "admin", "ui" });
			} else {
				return null;
			}

		}

		@Override
		public boolean isUserInRole(final Scope scope, final String role) {
			System.out.println("role " + role);
			return true;
		}

		@Override
		public void logout() {
			// TODO: remove any acquired tokens.

		}

	}

}
