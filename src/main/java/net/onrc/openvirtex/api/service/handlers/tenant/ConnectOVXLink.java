/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
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
	    final Number tenantId = HandlerUtils.<Number> fetchField(
		    TenantHandler.TENANT, params, true, null);
	    final Number srcDpid = HandlerUtils.<Number> fetchField(
		    TenantHandler.SRC_DPID, params, true, null);
	    final Number srcPort = HandlerUtils.<Number> fetchField(
		    TenantHandler.SRC_PORT, params, true, null);
	    final Number dstDpid = HandlerUtils.<Number> fetchField(
		    TenantHandler.DST_DPID, params, true, null);
	    final Number dstPort = HandlerUtils.<Number> fetchField(
		    TenantHandler.DST_PORT, params, true, null);
	    final String pathString = HandlerUtils.<String> fetchField(
		    TenantHandler.PATH, params, true, null);
	    final Number priority = HandlerUtils.<Number> fetchField(
		    TenantHandler.PRIORITY, params, true, null);

	    HandlerUtils.isValidTenantId(tenantId.intValue());
	    HandlerUtils.isValidOVXSwitch(tenantId.intValue(),
		    srcDpid.longValue());
	    HandlerUtils.isValidOVXSwitch(tenantId.intValue(),
		    dstDpid.longValue());
	    HandlerUtils.isValidOVXPort(tenantId.intValue(),
		    srcDpid.longValue(), srcPort.shortValue());
	    HandlerUtils.isValidOVXPort(tenantId.intValue(),
		    dstDpid.longValue(), dstPort.shortValue());
	    HandlerUtils.isUsedOVXPort(tenantId.intValue(),
		    srcDpid.longValue(), srcPort.shortValue());
	    HandlerUtils.isUsedOVXPort(tenantId.intValue(),
		    dstDpid.longValue(), dstPort.shortValue());
	    final List<PhysicalLink> physicalLinks = HandlerUtils
		    .getPhysicalPath(pathString);
	    HandlerUtils.isValidVirtualLink(physicalLinks);
	    HandlerUtils.areValidLinkEndPoints(tenantId.intValue(),
		    srcDpid.longValue(), srcPort.shortValue(),
		    dstDpid.longValue(), dstPort.shortValue(), physicalLinks);

	    final OVXMap map = OVXMap.getInstance();
	    final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
		    .intValue());
	    final OVXLink virtualLink = virtualNetwork.connectLink(
		    srcDpid.longValue(), srcPort.shortValue(),
		    dstDpid.longValue(), dstPort.shortValue(), physicalLinks,
		    priority.byteValue());
	    if (virtualLink == null) {
		resp = new JSONRPC2Response(-1, 0);
	    } else {
		this.log.info(
		        "Created bi-directional virtual link {} between ports {}/{} - {}/{} in virtual network {}",
		        virtualLink.getLinkId(), virtualLink.getSrcSwitch()
		                .getSwitchName(), virtualLink.getSrcPort()
		                .getPortNumber(), virtualLink.getDstSwitch()
		                .getSwitchName(), virtualLink.getDstPort()
		                .getPortNumber(), virtualNetwork.getTenantId());
		resp = new JSONRPC2Response(virtualLink.getLinkId(), 0);
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
