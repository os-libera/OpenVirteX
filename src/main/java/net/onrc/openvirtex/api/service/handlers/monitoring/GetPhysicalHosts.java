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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Handler to get a list of physical hosts.
 */
public class GetPhysicalHosts extends ApiHandler<Object> {

    @Override
    public JSONRPC2Response process(Object params) {

        JSONRPC2Response resp = null;

        try {
            List<Object> hosts = new LinkedList<Object>();
            OVXMap map = OVXMap.getInstance();
            Collection<OVXNetwork> vnets = map.listVirtualNetworks().values();

            for (OVXNetwork vnet : vnets) {
                for (Host h : vnet.getHosts()) {
                    hosts.add(h.convertToPhysical());
                }
            }

            resp = new JSONRPC2Response(hosts, 0);
        } catch (ClassCastException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName() + ": Unable to fetch host list : "
                                    + e.getMessage()), 0);
        }
        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.NO_PARAMS;
    }

}
