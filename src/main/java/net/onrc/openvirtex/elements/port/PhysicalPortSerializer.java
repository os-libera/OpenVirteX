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
package net.onrc.openvirtex.elements.port;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * serializer for PhysicalPorts.
 */
public class PhysicalPortSerializer extends JsonSerializer<PhysicalPort> {

    @Override
    public void serialize(final PhysicalPort port, final JsonGenerator jgen,
            final SerializerProvider sp) throws IOException,
            JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("dpid", port.getParentSwitch().getSwitchName());
        jgen.writeStringField("port", String.valueOf(port.getPortNumber()));
        jgen.writeEndObject();
    }

}
