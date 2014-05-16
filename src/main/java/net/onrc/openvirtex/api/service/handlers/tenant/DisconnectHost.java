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
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.InvalidHostException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Handler to disconnect a host.
 */
public class DisconnectHost extends ApiHandler<Map<String, Object>> {

    private Logger log = LogManager.getLogger(DisconnectHost.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        JSONRPC2Response resp = null;

        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final Number hostId = HandlerUtils.<Number>fetchField(
                    TenantHandler.HOST, params, true, null);

            HandlerUtils.isValidTenantId(tenantId.intValue());
            HandlerUtils.isValidHostId(tenantId.intValue(), hostId.intValue());

            final OVXMap map = OVXMap.getInstance();
            final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                    .intValue());

            virtualNetwork.disconnectHost(hostId.intValue());

            this.log.info("Disconnected host {} in virtual network {}", hostId,
                    tenantId);
            resp = new JSONRPC2Response(0);

        } catch (final MissingRequiredField e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName() + ": Unable to disconnect host : "
                                    + e.getMessage()), 0);
        } catch (final InvalidTenantIdException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenant id : " + e.getMessage()), 0);
        } catch (final InvalidHostException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid host id : " + e.getMessage()), 0);
        } catch (final NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        }

        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

}
