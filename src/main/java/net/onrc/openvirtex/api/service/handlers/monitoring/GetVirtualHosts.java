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

import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.host.HostSerializer;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetVirtualHosts extends ApiHandler<Map<String, Object>> {

    @SuppressWarnings("unchecked")
    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        List<Object> result;
        JSONRPC2Response resp = null;
        Number tid = null;
        try {
            tid = HandlerUtils.<Number>fetchField(MonitoringHandler.TENANT,
                    params, true, null);
            final OVXNetwork vnet = OVXMap.getInstance().getVirtualNetwork(
                    tid.intValue());

            // TODO: gson objects can be shared with other methods
            final GsonBuilder gsonBuilder = new GsonBuilder();
            // gsonBuilder.setPrettyPrinting();
            gsonBuilder.registerTypeAdapter(Host.class, new HostSerializer());
            final Gson gson = gsonBuilder.create();
            // TODO Fix this GSON crazy shit.

            result = gson.fromJson(gson.toJson(vnet.getHosts()), List.class);
            resp = new JSONRPC2Response(result, 0);
        } catch (ClassCastException | MissingRequiredField e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName() + ": Unable to fetch host list : "
                                    + e.getMessage()), 0);
        } catch (NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenantId : " + tid), 0);
        }
        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

}
