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

import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.UserIdentity.Scope;

/**
 * Implements a login service by checking username and password and mapping
 * users to their role/resource.
 */
public class OVXLoginService implements LoginService {

    private final IdentityService identityService = new DefaultIdentityService();

    @Override
    public IdentityService getIdentityService() {
        return this.identityService;
    }

    @Override
    public String getName() {
        return JettyServer.REALM;
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

    /**
     * An authenticated user has an identity and can be logged out.
     */
    public class OpenVirteXAuthenticatedUser implements Authentication.User {

        private final String user;
        @SuppressWarnings("unused")
        private final String password;

        /**
         * Creates an authenticated user.
         *
         * @param username
         *            the user's name
         * @param password
         *            the user's password
         */
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
                        this.user), new String[] {"user"});
            } else if (this.user.equals("ui")) {
                return new DefaultUserIdentity(new Subject(), new JMXPrincipal(
                        this.user), new String[] {"ui"});
            } else if (this.user.equals("admin")) {
                return new DefaultUserIdentity(new Subject(), new JMXPrincipal(
                        this.user), new String[] {"user", "admin", "ui"});
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
