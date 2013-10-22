/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class CreateOVXLink extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(CreateOVXLink.class.getName());

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
	JSONRPC2Response resp = null;

	try {
	    final Number tenantId = HandlerUtils.<Number> fetchField(
		    TenantHandler.TENANT, params, true, null);
	    final String pathString = HandlerUtils.<String> fetchField(
		    TenantHandler.PATH, params, true, null);

	    // TODO: should create parsePath() in HandlerUtils as similar
	    // functionality is used in createOVXSwitchRoute
	    final List<PhysicalLink> physicalLinks = new LinkedList<PhysicalLink>();
	    for (final String hop : pathString.split(",")) {
		final String srcString = hop.split("-")[0];
		final String dstString = hop.split("-")[1];
		final String[] srcDpidPort = srcString.split("/");
		final String[] dstDpidPort = dstString.split("/");
		final long srcDpid = Long.parseLong(srcDpidPort[0]);
		final long dstDpid = Long.parseLong(dstDpidPort[0]);
		final PhysicalPort srcPort = PhysicalNetwork.getInstance()
			.getSwitch(srcDpid)
			.getPort(Short.valueOf(srcDpidPort[1]));
		final PhysicalPort dstPort = PhysicalNetwork.getInstance()
			.getSwitch(dstDpid)
			.getPort(Short.valueOf(dstDpidPort[1]));
		final PhysicalLink link = PhysicalNetwork.getInstance()
			.getLink(srcPort, dstPort);
		physicalLinks.add(link);
	    }
	    HandlerUtils.isValidTenantId(tenantId.intValue());
	    HandlerUtils
	    .isVirtualLinkUnique(tenantId.intValue(), physicalLinks);

	    // TODO: virtualLinkUnique should check if the physical topology
	    // allows for the path that has been specified
	    // isValidLink(physicalLinks, pathString);
	    final OVXMap map = OVXMap.getInstance();
	    final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
		    .intValue());
	    final OVXLink virtualLink = virtualNetwork
		    .createLink(physicalLinks);
	    if (virtualLink == null) {
		resp = new JSONRPC2Response(-1, 0);
	    } else {
		this.log.info("Created virtual link {} in virtual network {}",
			virtualLink.getLinkId(), virtualNetwork.getTenantId());
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
	    resp = new JSONRPC2Response(new JSONRPC2Error(
		    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
		    + ": Impossible to create the virtual link, too many links in this virtual network : " + e.getMessage()), 0);
	} catch (NetworkMappingException e) {
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
