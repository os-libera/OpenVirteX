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

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class OVXPortSerializer implements JsonSerializer<OVXPort> {

    @Override
    public JsonElement serialize(final OVXPort port, final Type portType,
            final JsonSerializationContext context) {
        final JsonObject result = new JsonObject();
        result.addProperty("dpid", port.getParentSwitch().getSwitchName());
        result.addProperty("port", String.valueOf(port.getPortNumber()));
        return result;
    }

}
