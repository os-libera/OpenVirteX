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

import net.onrc.openvirtex.elements.datapath.Switch;

/**
 * Exception thrown when OVX/Physical switches or their attributes (ports) are
 * not found in a map.
 */
@SuppressWarnings("rawtypes")
public class SwitchMappingException extends MappingException {

    private static final long serialVersionUID = 798688L;

    public SwitchMappingException() {
        super();
    }

    public SwitchMappingException(String cause) {
        super(cause);
    }

    public SwitchMappingException(Integer key, Class value) {
        super(value.getName() + " not found for tenant with ID " + key);
    }

    public SwitchMappingException(Switch key, Class value) {
        super(value.getName() + " not found for switch [" + key.getSwitchId()
                + "]");
    }

    public SwitchMappingException(Object key, Class value) {
        super(key, value);
    }

    public SwitchMappingException(Throwable cause) {
        super(cause);
    }

}
