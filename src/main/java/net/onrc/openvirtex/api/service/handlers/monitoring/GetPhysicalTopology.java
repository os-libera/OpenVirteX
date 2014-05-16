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
package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitchSerializer;

import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.elements.port.PhysicalPortSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Gets the physical topology in json format.
 */
public class GetPhysicalTopology extends ApiHandler<Object> {

    @SuppressWarnings("unchecked")
    @Override
    public JSONRPC2Response process(final Object params) {
        Map<String, Object> result;
        JSONRPC2Response resp = null;
        // TODO: gson objects can be shared with other methods
        final GsonBuilder gsonBuilder = new GsonBuilder();
        // gsonBuilder.setPrettyPrinting();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        gsonBuilder.registerTypeAdapter(PhysicalSwitch.class,
                new PhysicalSwitchSerializer());
        gsonBuilder.registerTypeAdapter(PhysicalPort.class,
                new PhysicalPortSerializer());
        /*
         * gsonBuilder.registerTypeAdapter(PhysicalLink.class, new
         * PhysicalLinkSerializer());
         */

        final Gson gson = gsonBuilder.create();

        result = gson.fromJson(gson.toJson(PhysicalNetwork.getInstance()),
                Map.class);
        resp = new JSONRPC2Response(result, 0);
        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.NO_PARAMS;
    }

}
