/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.api.service.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidLinkException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.LinkMappingException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;

public class HandlerUtils {

	@SuppressWarnings("unchecked")
	public static <T> T fetchField(final String fieldName,
			final Map<String, Object> map,
			/* Class<T> type, */final boolean required, final T def)
			throws ClassCastException, MissingRequiredField {
		final Object field = map.get(fieldName);
		if (field == null) {
			if (required) {
				throw new MissingRequiredField(fieldName);
			} else {
				return def;
			}
		}
		/*
		 * if (field.getClass().isAssignableFrom()) return type.cast(field);
		 */
		return (T) field;
		// throw new UnknownFieldType(fieldName, type.getName());

	}

	/**
	 * Check that the controller host and port that we are trying to connect
	 * with is not already being used by another virtual network in our system.
	 * No two virtual networks can have the same controller host and port.
	 * 
	 * @param controllerPort
	 * @param controllerAddress
	 * @throws ControllerUnavailableException
	 */
	public static void isControllerAvailable(final String controllerAddress,
		final int controllerPort) throws ControllerUnavailableException {
	    String newCtrl = "";
	    String oldCtrl = "";
	    try {
		InetAddress address = InetAddress.getByName(controllerAddress);
		newCtrl = address.getHostAddress();
	    } catch (UnknownHostException e) {
		newCtrl = controllerAddress;
	    } 
	    
	    
	    for (final OVXNetwork network : OVXMap.getInstance().getNetworkMap()
		    .values()) {
		final int port = network.getControllerPort();
		final String host = network.getControllerHost();
		try {
		    InetAddress address = InetAddress.getByName(host);
		    oldCtrl = address.getHostAddress();
		} catch (UnknownHostException e) {
		    oldCtrl = host;
		}
		if (port == controllerPort && newCtrl.equals(oldCtrl)) {
		    throw new ControllerUnavailableException(
			    "The controller we are trying to connect is already in use: "
				    + String.valueOf(controllerPort) + " "
				    + controllerAddress);
		}
	    }
	}

	/**
	 * Check that the tenant id specified refers to a virtual network in the
	 * system.
	 * 
	 * @param tenantId
	 * @throws InvalidTenantIdException
	 */
	public static void isValidTenantId(final int tenantId)
			throws InvalidTenantIdException {
		final OVXMap map = OVXMap.getInstance();
		try {
		    map.getVirtualNetwork(tenantId);
		} catch (NetworkMappingException e) {
			throw new InvalidTenantIdException(
					"The tenant id you have provided does not refer to a virtual network. TenantId: "
							+ String.valueOf(tenantId));
		}
	}
	
	/**
	 * Check that the link id specified refers to a pair of virtual links in the
	 * virtual network.
	 * 
	 * @param tenantId
	 * @param linkId
	 * @throws InvalidLinkException
	 */
	public static void isValidLinkId(final int tenantId, final int linkId)
		throws InvalidTenantIdException {
	    final OVXMap map = OVXMap.getInstance();
	    OVXNetwork virtualNetwork;
            try {
	        virtualNetwork = map.getVirtualNetwork(tenantId);
            } catch (NetworkMappingException e) {
        	throw new InvalidTenantIdException(
			"The tenant id you have provided does not refer to a virtual network. TenantId: "
					+ String.valueOf(tenantId));
            }   
	    LinkedList<OVXLink> linkList = virtualNetwork.getLinksById(linkId);
	    if (linkList == null) {
		throw new InvalidLinkException(
			"The link id you have provided does not refer to a virtual link. TenantId: "
				+ String.valueOf(tenantId) + ". LinkId: " + String.valueOf(linkId));
	    }
	}

	/**
	 * Check that the switch id specified belongs to the virtual network
	 * 
	 * @param tenantId
	 * @param dpid
	 * @throws InvalidDPIDException
	 */
	public static void isValidOVXSwitch(final int tenantId, final long dpid) {
	    final OVXMap map = OVXMap.getInstance();
	    OVXNetwork virtualNetwork;
            try {
	        virtualNetwork = map.getVirtualNetwork(tenantId);
            } catch (NetworkMappingException e) {
        	throw new InvalidTenantIdException(
			"The tenant id you have provided does not refer to a virtual network. TenantId: "
					+ String.valueOf(tenantId));
            }   
	    OVXSwitch sw = virtualNetwork.getSwitch(dpid);
	    if (sw == null) {
		throw new InvalidDPIDException(
			"The switch id you have provided does not belong to this virtual network: "
				+ String.valueOf(tenantId));
	    }
	}
	
	/**
	 * Check that the physical dpids that are provided all actually refer to a
	 * physical switch in the physical network. If any of them does not exist
	 * then we can throw an exception.
	 * 
	 * @param tenantId
	 * @param dpids
	 * @throws InvalidDPIDException
	 */
	public static void isValidDPID(final int tenantId, final List<Long> dpids)
			throws InvalidDPIDException {
		if (dpids.size() < 1) {
			throw new InvalidDPIDException(
					"You did not provide any physical dpids. "
							+ "This must be provided in order to create a virtual switch");
		}
		final PhysicalNetwork physicalNetwork = PhysicalNetwork.getInstance();
		// If any of the physical dpids that have been specified don't exist
		// then we should throw an error
		for (final long dpid : dpids) {
			final PhysicalSwitch sw = physicalNetwork.getSwitch(dpid);
			if (sw == null) {
				throw new InvalidDPIDException(
						"One of the physical dpids that you have provided "
								+ "does not refer to a switch in the physical plane. DPID: "
								+ String.valueOf(dpid));
			}

			try {
				OVXSwitch vsw = OVXMap.getInstance().getVirtualSwitch(sw, tenantId);
				if (vsw != null) {
				throw new InvalidDPIDException(
            				"The physical dpid is already part of a "
            				+ "virtual switch in the virtual network you have specified. dpid: "
            				+ String.valueOf(dpid));
			    	}
                        } catch (SwitchMappingException e) {
                        }
		}
	}

	/**
	 * Check if the ovx port number specified is present on the virtual
	 * switch, 
	 * 
	 * @param tenantId
	 * @param dpid
	 * @param portNumber
	 * @throws InvalidPortException
	 * @throws NetworkMappingException 
	 */
	public static void isValidOVXPort(final int tenantId, final long dpid,
		final short portNumber) throws InvalidPortException, NetworkMappingException {
	    final OVXSwitch sw = OVXMap.getInstance().getVirtualNetwork(tenantId).getSwitch(dpid);
	    if (sw == null || sw.getPort(portNumber) == null) {
		throw new InvalidPortException(
			"The ovx port specified is invalid: tenantId, dpid, port - "
				+ String.valueOf(tenantId) + ", "
				+ String.valueOf(dpid) + ", "
				+ String.valueOf(portNumber));
	    }
	}
	
	/**
	 * Check if the physical port number specified is present on the physical
	 * switch, and that this physical port is actually an edge port on the
	 * physical network.
	 * 
	 * @param tenantId
	 * @param dpid
	 * @param portNumber
	 * @throws InvalidPortException
	 */
	public static void isValidEdgePort(final int tenantId, final long dpid,
			final short portNumber) throws InvalidPortException {
		final PhysicalSwitch sw = PhysicalNetwork.getInstance().getSwitch(dpid);
		if (sw == null || sw.getPort(portNumber) == null
				|| !sw.getPort(portNumber).isEdge()) {
			throw new InvalidPortException(
					"The port specified is invalid: tenantId, dpid, port - "
							+ String.valueOf(tenantId) + ", "
							+ String.valueOf(dpid) + ", "
							+ String.valueOf(portNumber));
		}
	}

	/**
	 * Check that the virtual link we are trying to create is the only virtual
	 * link in the virtual network which has the same physical hops that have
	 * been specified.
	 * 
	 * This method will iterate over all physical links in the path. On every
	 * iteration, we find the virtual links that use this physical link, and
	 * take the intersection of current and previous virtual link sets. If the
	 * intersection is not empty, the virtual link is not unique.
	 * 
	 * @throws VirtualLinkException
	 */
	public static void isVirtualLinkUnique(final int tenantId,
			final List<PhysicalLink> physicalLinks) throws VirtualLinkException {
		final OVXMap map = OVXMap.getInstance();

		// Get virtual links that also use first hop of physical path
		final List<OVXLink> intersection = fetchOVXLink(map, physicalLinks.get(0), tenantId);
		if (intersection == null) {
			return;
		}

		// Find vlinks which also contain the remaining physical hops
		for (final PhysicalLink link : physicalLinks) {
			final List<OVXLink> overlap = fetchOVXLink(map, link, tenantId);
			if (overlap == null) {
				return;
			}
			intersection.retainAll(overlap);
			if (intersection.size() == 0) {
				return;
			}
		}

		// Check for cases where new virtual link is strict subset of existing
		// virtual links
		final Iterator<OVXLink> iter = intersection.iterator();
		while (iter.hasNext()) {
			final OVXLink vlink = iter.next();
			// Check physical path lengths
			List<PhysicalLink> path;
                        try {
	                    path = map.getPhysicalLinks(vlink);
                        } catch (LinkMappingException e) {
	                    throw new RuntimeException("Unexpected Inconsistency in OXVMap: " + e.getMessage());
                        }
			if (path.size() != physicalLinks.size()) {
				iter.remove();
				continue;
			} else {
				// Check if paths are equal
				for (int i = 0; i < physicalLinks.size(); i++) {
					if (physicalLinks.get(i) != path.get(i)) {
						iter.remove();
						break;
					}
				}
			}
		}
		if (intersection.size() == 0) {
			return;
		}

		throw new VirtualLinkException(
				"Virtual link already exists. cannot create the same virtual link in the same virtual network.");
	}
	
	protected static List<OVXLink> fetchOVXLink(Mappable map, PhysicalLink phyLink, int tenantId) {
	    	if (map.hasOVXLinks(phyLink, tenantId)) {
		    try {
	                return map.getVirtualLinks(phyLink, tenantId);
		    } catch (LinkMappingException e) {
	                throw new RuntimeException("Unexpected Inconsistency in OXVMap: " + e.getMessage());
		    }
		} else {
			return null;
		}
	}
}
