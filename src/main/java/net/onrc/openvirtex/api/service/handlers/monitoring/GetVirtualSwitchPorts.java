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


import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.MonitoringHandler;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetVirtualSwitchPorts extends ApiHandler<Map<String, Object>> {

    private JSONRPC2Response resp = null;

    
    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        try {
        	final Number tid = HandlerUtils.<Number>fetchField(MonitoringHandler.TENANT,
                    params, true, null);
            final Number dpid = HandlerUtils.<Number>fetchField(
                    MonitoringHandler.VDPID, params, true, -1);
            OVXSwitch sw = OVXMap.getInstance().getVirtualNetwork(tid.intValue()).getSwitch(dpid.longValue());
            
            Map<Short, Object> ret = new HashMap<Short, Object>();
            Map<String, Object> phys;
            for (Map.Entry<Short, OVXPort> entry : sw.getPorts().entrySet()) {
            	phys = new HashMap<String, Object>();
            	phys.put(MonitoringHandler.DPID, entry.getValue().getPhysicalPort().getParentSwitch().getSwitchName());
            	phys.put(TenantHandler.PORT, entry.getValue().getPhysicalPortNumber());
            	ret.put(entry.getKey(), phys);
            }
            
            
            return new JSONRPC2Response(ret, 0);
            

        } catch (ClassCastException | MissingRequiredField e) {
            this.resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to fetch virtual switch : "
                            + e.getMessage()), 0);
        } catch (final InvalidDPIDException e) {
            this.resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to fetch virtual switch : "
                            + e.getMessage()), 0);
        } catch (NetworkMappingException e) {
        	this.resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to fetch virtual network : "
                            + e.getMessage()), 0);
		}

        return this.resp;

    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

   
}
