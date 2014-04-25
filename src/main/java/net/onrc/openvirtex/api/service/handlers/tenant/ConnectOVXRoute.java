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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidPriorityException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ConnectOVXRoute extends ApiHandler<Map<String, Object>> {
    final Logger log = LogManager.getLogger(ConnectOVXRoute.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {

        JSONRPC2Response resp = null;

        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final Number dpid = HandlerUtils.<Long>fetchField(
                    TenantHandler.VDPID, params, true, null);
            final Number srcPort = HandlerUtils.<Number>fetchField(
                    TenantHandler.SRC_PORT, params, true, null);
            final Number dstPort = HandlerUtils.<Number>fetchField(
                    TenantHandler.DST_PORT, params, true, null);
            final String pathString = HandlerUtils.<String>fetchField(
                    TenantHandler.PATH, params, true, null);
            final Number priority = HandlerUtils.<Number>fetchField(
                    TenantHandler.PRIORITY, params, true, null);

            HandlerUtils.isValidTenantId(tenantId.intValue());
            // TODO: add check for bigswitch routing algorithm and deny if
            // non-manual (e.g., spf)?
            HandlerUtils.isValidOVXBigSwitch(tenantId.intValue(),
                    dpid.longValue());
            HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(),
                    srcPort.shortValue());
            HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(),
                    dstPort.shortValue());
            final List<PhysicalLink> physicalLinks = HandlerUtils
                    .getPhysicalPath(pathString);
            HandlerUtils.isValidVirtualLink(physicalLinks);
            HandlerUtils.isValidPriority(priority.intValue());

            final OVXMap map = OVXMap.getInstance();
            final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                    .intValue());

            final SwitchRoute virtualRoute = virtualNetwork.connectRoute(
                    dpid.longValue(), srcPort.shortValue(),
                    dstPort.shortValue(), physicalLinks, priority.byteValue());
            if (virtualRoute == null) {
                resp = new JSONRPC2Response(
                        new JSONRPC2Error(
                                JSONRPC2Error.INTERNAL_ERROR.getCode(),
                                this.cmdName()), 0);
            } else {
                this.log.info(
                        "Created bi-directional virtual route {} between ports ({},{}) on virtual big-switch {} in virtual network {}",
                        virtualRoute.getRouteId(), virtualRoute.getSrcPort()
                                .getPortNumber(), virtualRoute.getDstPort()
                                .getPortNumber(), virtualRoute.getSrcSwitch()
                                .getSwitchName(), virtualNetwork.getTenantId());
                Map<String, Object> reply = new HashMap<String, Object>(
                        virtualRoute.getDBObject());
                reply.put(TenantHandler.TENANT, virtualRoute.getTenantId());
                resp = new JSONRPC2Response(reply, 0);
            }
        } catch (final MissingRequiredField e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to create virtual switch route : "
                            + e.getMessage()), 0);
        } catch (final InvalidTenantIdException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenant id : " + e.getMessage()), 0);
        } catch (final InvalidDPIDException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName() + ": Invalid virtual switch id : "
                                    + e.getMessage()), 0);
        } catch (final VirtualLinkException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid virtual route : " + e.getMessage()), 0);
        } catch (final InvalidPortException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid virtual port id : " + e.getMessage()),
                    0);
        } catch (final IndexOutOfBoundException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(
                            JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName()
                                    + ": Unable to create the virtual switch route, too many routes in this virtual switch : "
                                    + e.getMessage()), 0);
        } catch (final NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        } catch (final InvalidPriorityException e) {
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
