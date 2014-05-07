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
 * A exception for errors thrown when a mapping cannot find an element i.e. a
 * provided key is not mapped to any value. Intended to be a superclass for a
 * class of Mappable-related fetch failures that return null.
 */
@SuppressWarnings("rawtypes")
public class MappingException extends Exception {
    /* OVX */
    private static final long serialVersionUID = 798688L;

    MappingException() {
        super();
    }

    MappingException(String cause) {
        super(cause);
    }

    public MappingException(Object key, Class value) {
        super(value.getName() + " not found for key: \n\t"
                + key.getClass().getName() + ":\n\t[" + key.toString() + "]");
    }

    MappingException(Throwable cause) {
        super(cause);
    }

}
