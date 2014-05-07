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

import net.onrc.openvirtex.elements.address.IPAddress;

/**
 * Exception thrown when addresses are not found in mappings. Addresses include
 * IP(virtual and physical) and hardware addresses (MACs).
 */
@SuppressWarnings("rawtypes")
public class AddressMappingException extends MappingException {

    private static final long serialVersionUID = 798688L;

    public AddressMappingException() {
        super();
    }

    public AddressMappingException(String cause) {
        super(cause);
    }

    public AddressMappingException(Integer key, Class value) {
        super(value.getName() + " not found for tenant with ID " + key);
    }

    public AddressMappingException(IPAddress key, Class value) {
        super(value.getName() + " not found for " + key);
    }

    public AddressMappingException(Throwable cause) {
        super(cause);
    }

}
