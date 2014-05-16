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
 * Exception thrown when tenant networks are not found in the map.
 */
@SuppressWarnings("rawtypes")
public class NetworkMappingException extends MappingException {

    private static final long serialVersionUID = 798688L;

    public NetworkMappingException() {
        super();
    }

    public NetworkMappingException(String cause) {
        super(cause);
    }

    public NetworkMappingException(Integer key) {
        super("Virtual network not found for tenant with ID " + key);
    }

    public NetworkMappingException(Object key, Class value) {
        super(key, value);
    }

    public NetworkMappingException(Throwable cause) {
        super(cause);
    }

}
