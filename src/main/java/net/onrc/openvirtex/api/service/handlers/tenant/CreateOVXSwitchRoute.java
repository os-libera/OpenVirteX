/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.routing.RoutingAlgorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class CreateOVXSwitchRoute extends ApiHandler<Map<String, Object>> {

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		final Logger log = LogManager.getLogger(CreateOVXSwitchRoute.class
				.getName());
		JSONRPC2Response resp = null;

		try {
			final Number tenantId = HandlerUtils.<Number> fetchField(
					TenantHandler.TENANT, params, true, null);
			final Number dpid = HandlerUtils.<Long> fetchField(
					TenantHandler.DPID, params, true, null);
			final Number inPort = HandlerUtils.<Number> fetchField(
					TenantHandler.SRC_PORT, params, true, null);
			final Number outPort = HandlerUtils.<Number> fetchField(
					TenantHandler.DST_PORT, params, true, null);
			final String path = HandlerUtils.<String> fetchField(
					TenantHandler.PATH, params, true, null);

			HandlerUtils.isValidTenantId(tenantId.intValue());
			HandlerUtils.isValidOVXSwitch(tenantId.intValue(), dpid.longValue());
			HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(), inPort.shortValue());
			HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(), outPort.shortValue());

			final OVXMap map = OVXMap.getInstance();
			final OVXNetwork virtNetwork = map.getVirtualNetwork(tenantId
					.intValue());
			final PhysicalNetwork phyNetwork = PhysicalNetwork.getInstance();
			final OVXSwitch virtSwitch = virtNetwork
					.getSwitch(dpid.longValue());

			// only try route setup if it's a BigSwitch
			if (!(virtSwitch instanceof OVXBigSwitch)) {
				resp = new JSONRPC2Response(new JSONRPC2Error(
						JSONRPC2Error.INTERNAL_ERROR.getCode(), this.cmdName()
								+ ": invalid switch type"), 0);
				return resp;
			}

			final OVXBigSwitch bigSwitch = (OVXBigSwitch) virtSwitch;
			// only allow if algorithm is NONE
			if (!bigSwitch.getAlg().equals(RoutingAlgorithms.NONE)) {
				resp = new JSONRPC2Response(new JSONRPC2Error(
						JSONRPC2Error.INTERNAL_ERROR.getCode(), this.cmdName()
								+ ": switch has internal routing"), 0);
				return resp;
			}
			final Set<PhysicalSwitch> switchSet = new HashSet<PhysicalSwitch>(
					bigSwitch.getMap().getPhysicalSwitches(bigSwitch));

			// find ingress/egress virtual ports to Big Switch
			final OVXPort ingress = virtSwitch.getPort(inPort.shortValue());
			final OVXPort egress = virtSwitch.getPort(outPort.shortValue());

			

			final List<PhysicalLink> pathLinks = new ArrayList<PhysicalLink>();
			final List<PhysicalLink> reverseLinks = new ArrayList<PhysicalLink>();

			// handle route string
			for (final String link : path.split(",")) {
				final String srcString = link.split("-")[0];
				final String dstString = link.split("-")[1];
				final String[] srcDpidPort = srcString.split("/");
				final String[] dstDpidPort = dstString.split("/");
				final long srcDpid = Long.parseLong(srcDpidPort[0]);
				final long dstDpid = Long.parseLong(dstDpidPort[0]);
				final PhysicalSwitch srcSwitch = phyNetwork.getSwitch(srcDpid);
				final PhysicalSwitch dstSwitch = phyNetwork.getSwitch(dstDpid);

				// if either source or dst switch don't exist, quit
				if (srcSwitch == null || dstSwitch == null) {
					resp = new JSONRPC2Response(new JSONRPC2Error(
							JSONRPC2Error.INTERNAL_ERROR.getCode(),
							this.cmdName() + ": invalid route"), 0);
					return resp;
				}

				// for each link, check if switch is part of big switch
				if (switchSet.contains(srcSwitch)
						&& switchSet.contains(dstSwitch)) {
					final PhysicalPort srcPort = srcSwitch.getPort(Short
							.valueOf(srcDpidPort[1]));
					final PhysicalPort dstPort = dstSwitch.getPort(Short
							.valueOf(dstDpidPort[1]));
					if (srcPort == null || dstPort == null) {
						resp = new JSONRPC2Response(new JSONRPC2Error(
								JSONRPC2Error.INTERNAL_ERROR.getCode(),
								this.cmdName() + ": invalid ports"), 0);
						return resp;
					}
					final PhysicalLink hop = phyNetwork.getLink(srcPort,
							dstPort);
					final PhysicalLink revhop = phyNetwork.getLink(dstPort,
							srcPort);
					pathLinks.add(hop);
					reverseLinks.add(revhop);
				} else {
					resp = new JSONRPC2Response(new JSONRPC2Error(
							JSONRPC2Error.INTERNAL_ERROR.getCode(),
							this.cmdName() + ": invalid route"), 0);
					return resp;
				}
			}
			Collections.reverse(reverseLinks);
			bigSwitch.createRoute(ingress, egress, pathLinks, reverseLinks);
			log.info("Created virtual switch route in virtual network {}",
					virtNetwork.getTenantId());
			resp = new JSONRPC2Response(virtSwitch.getSwitchId(), 0);
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
			resp = new JSONRPC2Response(new JSONRPC2Error(
				JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						+ ": Invalid virtual switch id : " + e.getMessage()), 0);
		} catch (final InvalidPortException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
				JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
						+ ": Invalid virtual port id : " + e.getMessage()), 0);
		} catch (final IndexOutOfBoundException e) {
		    resp = new JSONRPC2Response(new JSONRPC2Error(
			    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
			    + ": Impossible to create the virtual switch route, too many routes in this virtual switch : " + e.getMessage()), 0);
		}

		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}
}
