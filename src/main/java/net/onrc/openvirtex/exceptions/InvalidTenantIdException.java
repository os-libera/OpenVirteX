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
package net.onrc.openvirtex.exceptions;

/**
 * Admin tries to create a new virtual switches in a virtual network but the
 * tenantId that has been provided does not correspond to any tenantId that has
 * been created.
 */
public class InvalidTenantIdException extends IllegalArgumentException {

    private static final long serialVersionUID = 6957434977838246116L;

    public InvalidTenantIdException() {
        super();
    }

    public InvalidTenantIdException(final String msg) {
        super(msg);
    }

    public InvalidTenantIdException(final Throwable msg) {
        super(msg);
    }
}
