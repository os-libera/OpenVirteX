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
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.VirtualLinkException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ConnectOVXLink extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(ConnectOVXLink.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        JSONRPC2Response resp = null;

        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final Number srcDpid = HandlerUtils.<Number>fetchField(
                    TenantHandler.SRC_DPID, params, true, null);
            final Number srcPort = HandlerUtils.<Number>fetchField(
                    TenantHandler.SRC_PORT, params, true, null);
            final Number dstDpid = HandlerUtils.<Number>fetchField(
                    TenantHandler.DST_DPID, params, true, null);
            final Number dstPort = HandlerUtils.<Number>fetchField(
                    TenantHandler.DST_PORT, params, true, null);

            final String alg = HandlerUtils.<String>fetchField(
                    TenantHandler.ALGORITHM, params, true, null);
            final Number backupNumber = HandlerUtils.<Number>fetchField(
                    TenantHandler.BACKUPS, params, true, null);

            HandlerUtils.isValidTenantId(tenantId.intValue());
            HandlerUtils.isValidOVXSwitch(tenantId.intValue(),
                    srcDpid.longValue());
            HandlerUtils.isValidOVXSwitch(tenantId.intValue(),
                    dstDpid.longValue());
            HandlerUtils.isValidOVXPort(tenantId.intValue(),
                    srcDpid.longValue(), srcPort.shortValue());
            HandlerUtils.isValidOVXPort(tenantId.intValue(),
                    dstDpid.longValue(), dstPort.shortValue());

            final OVXMap map = OVXMap.getInstance();
            final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                    .intValue());

            final OVXLink virtualLink = virtualNetwork.connectLink(
                    srcDpid.longValue(), srcPort.shortValue(),
                    dstDpid.longValue(), dstPort.shortValue(), alg,
                    backupNumber.byteValue());


            if (virtualLink == null) {
                resp = new JSONRPC2Response(
                        new JSONRPC2Error(
                                JSONRPC2Error.INTERNAL_ERROR.getCode(),
                                this.cmdName()), 0);
            } else {
                this.log.info(
                        "Created bi-directional virtual link {} between ports {}/{} - {}/{} in virtual network {}",
                        virtualLink.getLinkId(), virtualLink.getSrcSwitch()
                                .getSwitchName(), virtualLink.getSrcPort()
                                .getPortNumber(), virtualLink.getDstSwitch()
                                .getSwitchName(), virtualLink.getDstPort()
                                .getPortNumber(), virtualNetwork.getTenantId());
                Map<String, Object> reply = new HashMap<String, Object>(
                        virtualLink.getDBObject());
                reply.put(TenantHandler.TENANT, virtualLink.getTenantId());
                resp = new JSONRPC2Response(reply, 0);
            }
        } catch (final MissingRequiredField e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to create virtual link : "
                            + e.getMessage()), 0);
        } catch (final VirtualLinkException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid virtual link : " + e.getMessage()), 0);
        } catch (final InvalidTenantIdException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenant id : " + e.getMessage()), 0);
        } catch (final IndexOutOfBoundException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(
                            JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName()
                                    + ": Impossible to create the virtual link, too many links in this virtual network : "
                                    + e.getMessage()), 0);
        } catch (final InvalidPortException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid port : " + e.getMessage()), 0);
        } catch (final InvalidDPIDException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName() + ": Invalid virtual switch id : "
                                    + e.getMessage()), 0);
        } catch (final MappingException e) {
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
