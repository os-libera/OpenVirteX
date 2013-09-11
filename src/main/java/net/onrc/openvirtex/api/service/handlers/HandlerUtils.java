package net.onrc.openvirtex.api.service.handlers;

import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
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
	public static <T> T fetchField(String fieldName, Map<String, Object> map, 
			/*Class<T> type,*/ boolean required, T def) 
					throws ClassCastException, MissingRequiredField {
		Object field = map.get(fieldName);
		if (field == null)
			if(required) 
				throw new MissingRequiredField(fieldName);
			else
				return def;
		/*if (field.getClass().isAssignableFrom()) 
			return type.cast(field);*/
		return (T) field;
		//throw new UnknownFieldType(fieldName, type.getName());

	}


	/**
	 * Check that the controller host and port that we are trying to connect with is not already
	 * being used by another virtual network in our system. No two virtual networks can have the
	 * same controller host and port.
	 * 
	 * @param controllerPort 
	 * @param controllerAddress 
	 * @throws ControllerUnavailableException
	 */
	public static void isControllerAvailable(String controllerAddress, int controllerPort) 
			throws ControllerUnavailableException {
		for (OVXNetwork network : OVXMap.getInstance().getNetworkMap().values()) {
			int port = network.getControllerPort();
			String host = network.getControllerHost();
			if (port == controllerPort && host.equals(controllerAddress)) {
				throw new ControllerUnavailableException(
						"The controller we are trying to connect is already in use: " 
								+ String.valueOf(controllerPort) + " " + controllerAddress);
			}
		}
	}

	/**
	 * Check that the tenant id specified refers to a virtual network in the system.
	 * 
	 * @param tenantId
	 * @throws InvalidTenantIdException
	 */
	public static void isValidTenantId(int tenantId) throws InvalidTenantIdException {
		final OVXMap map = OVXMap.getInstance();
		if (map.getVirtualNetwork(tenantId) == null) {
			throw new InvalidTenantIdException(
					"The tenant id you have provided does not refer to a virtual network. TenantId: "
							+ String.valueOf(tenantId));
		}
	}


	/**
	 * Check that the physical dpids that are provided all actually refer to a physical switch
	 * in the physical network. If any of them does not exist then we can throw an exception.
	 * 
	 * @param tenantId
	 * @param dpids
	 * @throws InvalidDPIDException
	 */
	public static void isValidDPID(int tenantId, List<Long> dpids) throws InvalidDPIDException {
		if (dpids.size() < 1){
			throw new InvalidDPIDException("You did not provide any physical dpids. "
					+ "This must be provided in order to create a virtual switch");
		}
		PhysicalNetwork physicalNetwork = PhysicalNetwork.getInstance();
		// If any of the physical dpids that have been specified don't exist then we should throw an error
		for (long dpid:dpids) {
			PhysicalSwitch sw = physicalNetwork.getSwitch(dpid);
			if (sw == null) {
				throw new InvalidDPIDException("One of the physical dpids that you have provided "
						+ "does not refer to a switch in the physical plane. DPID: " + String.valueOf(dpid));
			}

			if (OVXMap.getInstance().getVirtualSwitch(sw, tenantId) != null) {
				throw new InvalidDPIDException("The physical dpid is already part of a "
						+ "virtual switch in the virtual network you have specified. dpid: " + String.valueOf(dpid));
			}
		}
	}

	/**
	 * Check if the physical port number specified if present on the physical switch, and that this
	 * physical port is actually an edge port on the physical network.
	 *
	 * @param tenantId
	 * @param dpid
	 * @param portNumber
	 * @throws InvalidPortException
	 */
	public static void isValidEdgePort(int tenantId, long dpid, short portNumber) throws InvalidPortException {
		PhysicalSwitch sw = PhysicalNetwork.getInstance().getSwitch(dpid);
		if (sw == null || sw.getPort(portNumber) == null || ! sw.getPort(portNumber).isEdge()) {
			throw new InvalidPortException("The port specified is already being used: tenantId, dpid, port - " 
					+ String.valueOf(tenantId) + ", " + String.valueOf(dpid) 
					+ ", " + String.valueOf(portNumber));
		}
	}

	/**
	 * Check that the virtual link we are trying to create is the only virtual link in the 
	 * virtual network which has the same physical hops that have been specified.
	 * 
	 * @param virtualLink
	 * @throws VirtualLinkException
	 */
	public static void isVirtualLinkUnique(List<PhysicalLink> physicalLinks, String pathString) throws VirtualLinkException {
		final OVXMap map = OVXMap.getInstance();
		for (List<PhysicalLink> links : map.getVirtualLinkMap().values()) {
			if (links.size() != physicalLinks.size()) continue;
			int counter = 0;
			for(int i =0; i < links.size(); i ++) {
				if (links.get(i).equals(physicalLinks.get(i))) {
					counter = counter + 1;
				} else {
					break;
				}
			}
			if (counter == links.size()) {
				throw new VirtualLinkException("Virtual link already exists. cannot create the same virtual link in the same virtual network. physical hops: " + pathString);
			}
		}
	}


}
