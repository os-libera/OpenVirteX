package net.onrc.openvirtex.api.service.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
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
		for (final OVXNetwork network : OVXMap.getInstance().getNetworkMap()
				.values()) {
			final int port = network.getControllerPort();
			final String host = network.getControllerHost();
			if (port == controllerPort && host.equals(controllerAddress)) {
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
		if (map.getVirtualNetwork(tenantId) == null) {
			throw new InvalidTenantIdException(
					"The tenant id you have provided does not refer to a virtual network. TenantId: "
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

			if (OVXMap.getInstance().getVirtualSwitch(sw, tenantId) != null) {
				throw new InvalidDPIDException(
						"The physical dpid is already part of a "
								+ "virtual switch in the virtual network you have specified. dpid: "
								+ String.valueOf(dpid));
			}
		}
	}

	/**
	 * Check if the physical port number specified if present on the physical
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
					"The port specified is already being used: tenantId, dpid, port - "
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
		final List<OVXLink> intersection = map.getVirtualLinks(
				physicalLinks.get(0), tenantId);
		if (intersection == null) {
			return;
		}

		// Find intersection of remaining physical hops
		final int pathIndex = 1;
		while (intersection.size() > 0 && pathIndex < physicalLinks.size()) {
			final List<OVXLink> vlinks = map.getVirtualLinks(
					physicalLinks.get(pathIndex), tenantId);
			intersection.retainAll(vlinks);
		}
		if (intersection.size() == 0) {
			return;
		}

		// Check for cases where new virtual link is strict subset of existing
		// virtual links
		final Iterator<OVXLink> iter = intersection.iterator();
		while (iter.hasNext()) {
			final OVXLink vlink = iter.next();
			// Check physical path lengths
			final List<PhysicalLink> path = map.getPhysicalLinks(vlink);
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
}
