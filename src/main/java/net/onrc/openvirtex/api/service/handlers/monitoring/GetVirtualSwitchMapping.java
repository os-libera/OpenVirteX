/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.monitoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetVirtualSwitchMapping extends ApiHandler<Map<String, Object>> {

    JSONRPC2Response resp = null;

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        try {
            final Map<String, Object> res = new HashMap<String, Object>();
            final Number tid = HandlerUtils.<Number> fetchField(
                    MonitoringHandler.TENANT, params, true, null);
            final OVXMap map = OVXMap.getInstance();
            final LinkedList<String> list = new LinkedList<String>();
            final HashMap<String, Object> subRes = new HashMap<String, Object>();
            for (final OVXSwitch vsw : map.getVirtualNetwork(tid.intValue())
                    .getSwitches()) {
                subRes.clear();
                list.clear();
                if (vsw instanceof OVXBigSwitch) {
                    final List<Integer> l = new LinkedList<Integer>();
                    for (final PhysicalLink li : ((OVXBigSwitch) vsw)
                            .getAllLinks()) {
                        l.add(li.getLinkId());
                    }
                    subRes.put("links", l);
                } else {
                    subRes.put("links", new LinkedList<>());
                }
                for (final PhysicalSwitch psw : map.getPhysicalSwitches(vsw)) {
                    list.add(psw.getSwitchName());
                }
                subRes.put("switches", list.clone());
                res.put(vsw.getSwitchName(), subRes.clone());
            }
            this.resp = new JSONRPC2Response(res, 0);

        } catch (ClassCastException | MissingRequiredField
                | NullPointerException | NetworkMappingException
                | SwitchMappingException e) {
            this.resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to fetch virtual topology : "
                            + e.getMessage()), 0);
        }
        return this.resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }
}
