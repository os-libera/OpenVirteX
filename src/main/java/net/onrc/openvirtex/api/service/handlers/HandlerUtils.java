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
package net.onrc.openvirtex.api.service.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflow.util.HexString;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidHostException;
import net.onrc.openvirtex.exceptions.InvalidLinkException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidPriorityException;
import net.onrc.openvirtex.exceptions.InvalidRouteException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

/**
 * Utility class that implements various checks
 * to validate API input.
 */
public final class HandlerUtils {

    /**
     * Implements no-op private constructor.
     * Needed for checkstyle.
     */
    private HandlerUtils() {
    }

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
     * @param controllerAddress the controller address
     * @param controllerPort the controller port
     * @param tenantId the tenant ID
     * @throws ControllerUnavailableException if controller port and address are already in use
     */
    public static void isControllerAvailable(final String controllerAddress,
            final int controllerPort, int tenantId)
                    throws ControllerUnavailableException {
        String newCtrl = "";
        String oldCtrl = "";
        try {
            InetAddress address = InetAddress.getByName(controllerAddress);
            newCtrl = address.getHostAddress();
        } catch (UnknownHostException e) {
            newCtrl = controllerAddress;
        }

        for (final OVXNetwork network : OVXMap.getInstance()
                .listVirtualNetworks().values()) {
            if (tenantId == network.getTenantId()) {
                continue;
            }
            final Set<String> ctrlUrls = network.getControllerUrls();
            for (String url : ctrlUrls) {
                String[] urlParts = url.split(":");
                final int port = Integer.parseInt(urlParts[2]);
                final String host = urlParts[1];
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
    }

    /**
     * Checks that the tenant id specified refers to a virtual network in the
     * system.
     *
     * @param tenantId the tenant ID
     * @throws InvalidTenantIdException if tenant ID is invalid
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
     * Checks that the host ID specified refers to a valid host in the virtual
     * network.
     *
     * @param tenantId the tenant ID
     * @param hostId the host ID
     * @throws InvalidHostException
     * @throws NetworkMappingException
     */
    public static void isValidHostId(final int tenantId, final int hostId)
            throws InvalidHostException, NetworkMappingException {
        final OVXMap map = OVXMap.getInstance();
        final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
        Host host = virtualNetwork.getHost(hostId);
        if (host == null) {
            throw new InvalidHostException(
                    "The host id you have provided does not refer to a valid host. TenantId: "
                            + String.valueOf(tenantId) + ". HostId: "
                            + String.valueOf(hostId));
        }
    }

    /**
     * Checks that the link id specified refers to a pair of virtual links in the
     * virtual network.
     *
     * @param tenantId the tenant ID
     * @param linkId the link ID
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
        List<OVXLink> linkList = virtualNetwork.getLinksById(linkId);
        if (linkList == null) {
            throw new InvalidLinkException(
                    "The link id you have provided does not refer to a virtual link. TenantId: "
                            + String.valueOf(tenantId) + ". LinkId: "
                            + String.valueOf(linkId));
        }
    }

    /**
     * Checks that the route id specified refers to a pair of virtual route in
     * the big-switch belonging to the virtual network.
     *
     * @param tenantId the tenant ID
     * @param dpid the datapath ID
     * @param routeId the route ID
     * @throws InvalidRouteException
     * @throws NetworkMappingException
     */
    public static void isValidRouteId(final int tenantId, final long dpid,
            final int routeId) throws InvalidRouteException,
            NetworkMappingException {
        final OVXMap map = OVXMap.getInstance();
        final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
        OVXBigSwitch sw = (OVXBigSwitch) virtualNetwork.getSwitch(dpid);
        Set<SwitchRoute> routes = sw.getRoutebyId(routeId);
        if (routes.size() == 0) {
            throw new InvalidRouteException(
                    "The route id you have provided does not refer to a big-switch internal route. TenantId: "
                            + String.valueOf(tenantId)
                            + ". SwitchId: "
                            + String.valueOf(dpid)
                            + ". RouteId: "
                            + String.valueOf(routeId));
        }
    }

    /**
     * Checks that the switch id specified belongs to the virtual network.
     *
     * @param tenantId the tenant ID
     * @param dpid the datapath ID
     * @throws InvalidDPIDException
     */
    public static void isValidOVXSwitch(final int tenantId, final long dpid)
            throws InvalidTenantIdException, InvalidDPIDException {
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
     * Checks that the switch id specified belongs to the virtual network and is
     * of type OVXBigSwitch.
     *
     * @param tenantId the tenant ID
     * @param dpid the datapath ID
     * @throws NetworkMappingException
     * @throws InvalidDPIDException
     */
    public static void isValidOVXBigSwitch(final int tenantId, final long dpid)
            throws NetworkMappingException {
        final OVXMap map = OVXMap.getInstance();
        final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
        OVXSwitch sw = virtualNetwork.getSwitch(dpid);
        if (sw == null) {
            throw new InvalidDPIDException(
                    "The switch id you have provided does not belong to this virtual network: "
                            + String.valueOf(tenantId));
        }
        if (!(sw instanceof OVXBigSwitch)) {
            throw new InvalidDPIDException(
                    "The switch id you have provided doesn't belong to a big-switch: "
                            + String.valueOf(sw.getClass()));
        }
    }

    /**
     * Checks that the physical dpids that are provided all actually refer to a
     * physical switch in the physical network. If any of them does not exist
     * then we throw an exception. If a physical dpid is not connected to
     * any of the other ones, throw an exception. If physical dpid is already
     * mapped to a virtual switch then throw an exception.
     *
     * @param tenantId tenant ID
     * @param dpids the list of datapath IDs
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

            // Are all dpids connected - only relevant when creating a bigswitch
            if (dpids.size() > 1) {
                Set<PhysicalSwitch> neighbours = physicalNetwork.getNeighbors(sw);
                Set<Long> neighbourDpids = new HashSet<Long>();
                for (PhysicalSwitch neighbour : neighbours) {
                    neighbourDpids.add(neighbour.getSwitchId());
                }
                if (Collections.disjoint(dpids, neighbourDpids)) {
                    throw new InvalidDPIDException(
                            "One of the physical dpids you have provided is "
                                    + "disconnected from the others. DPID: "
                                    + String.valueOf(dpid));
                }
            }

            // Is the physical switch already used by another virtual switch?
            try {
                OVXSwitch vsw = OVXMap.getInstance().getVirtualSwitch(sw, tenantId);
                if (vsw != null) {
                    throw new InvalidDPIDException(
                            "The physical dpid is already part of a "
                                    + "virtual switch in the virtual network you have specified. DPID: "
                                    + String.valueOf(dpid));
                }
            } catch (SwitchMappingException e) {
                // No virtual switch maps to the given physical switch - this is what we want
                continue;
            }

        }
    }

    /**
     * Checks if the virtual port number specified is present on the virtual switch.
     *
     * @param tenantId the tenant ID
     * @param dpid the datapath ID
     * @param portNumber the port number
     * @throws InvalidPortException
     * @throws NetworkMappingException
     */
    public static void isValidOVXPort(final int tenantId, final long dpid,
            final short portNumber) throws InvalidPortException,
            NetworkMappingException {
        final OVXSwitch sw = OVXMap.getInstance().getVirtualNetwork(tenantId)
                .getSwitch(dpid);
        if (sw == null || sw.getPort(portNumber) == null) {
            throw new InvalidPortException(
                    "The ovx port specified is invalid: tenantId, dpid, port - "
                            + String.valueOf(tenantId) + ", "
                            + String.valueOf(dpid) + ", "
                            + String.valueOf(portNumber));
        }
    }

    /**
     * Checks if the priority specified is in the allowed range [0,127].
     *
     * @param priority the priority value
     * @throws InvalidPriorityException
     */
    public static void isValidPriority(final int priority)
            throws InvalidPriorityException {
        if (priority < 0 || priority > 127) {
            throw new InvalidPriorityException(
                    "The priority specified is invalid: allowed priorities are in range [0, 127]");
        }
    }

    /**
     * Checks if the host MAC address is not yet registered in the map.
     *
     * @param mac the MAC address
     * @throws InvalidPriorityException
     */
    public static void isUniqueHostMAC(final MACAddress mac)
            throws DuplicateMACException {
        if (OVXMap.getInstance().hasMAC(mac)) {
            throw new DuplicateMACException(
                    "The specified MAC address is already in use: "
                            + mac.toString());
        }
    }

    /**
     * Checks if the physical port number specified is present on the physical
     * switch.
     *
     * @param tenantId the tenant ID
     * @param dpid the datapath ID
     * @param portNumber the port number
     * @throws InvalidPortException
     * @throws SwitchMappingException
     */
    public static void isValidPhysicalPort(final int tenantId, final long dpid,
            final short portNumber) throws InvalidPortException,
            InvalidDPIDException, SwitchMappingException {
        final PhysicalSwitch sw = PhysicalNetwork.getInstance().getSwitch(dpid);
        if (sw == null || sw.getPort(portNumber) == null) {
            throw new InvalidPortException(
                    "The port specified is invalid: tenantId, dpid, port - "
                            + String.valueOf(tenantId) + ", "
                            + String.valueOf(dpid) + ", "
                            + String.valueOf(portNumber));
        }
        if (OVXMap.getInstance().getVirtualSwitch(sw, tenantId) == null) {
            throw new InvalidDPIDException(
                    "The physical dpid has first to be associated to "
                            + "virtual switch in the virtual network you have specified. dpid: "
                            + HexString.toHexString(dpid));
        }
    }

    /**
     * Checks that the virtual link we are trying to create is valid (e.g.,
     * dstSwitch of a physical Link is a SrcSwitch of the next physical link and
     * the physical ports are different).
     *
     * @param physicalLinks the path
     * @throws VirtualLinkException
     */

    public static void isValidVirtualLink(final List<PhysicalLink> physicalLinks)
            throws VirtualLinkException {
        PhysicalLink oldLink = null;
        for (PhysicalLink link : physicalLinks) {
            if (oldLink != null) {
                if (!oldLink.getDstSwitch().equals(link.getSrcSwitch())) {
                    throw new VirtualLinkException(
                            "Physical path not correct. Destination switch of one hop as to be equal to source switch "
                                    + "of the next hop, but "
                                    + oldLink.getDstSwitch().getSwitchName()
                                    + " != "
                                    + link.getSrcSwitch().getSwitchName());
                }
                if (oldLink.getDstPort().equals(link.getSrcPort())) {
                    throw new VirtualLinkException(
                            "Physical path not correct. Destination port of one hop as to be different to source port "
                                    + "of the next hop, but "
                                    + oldLink.getDstPort().getPortNumber()
                                    + " != "
                                    + link.getSrcPort().getPortNumber());
                }
            }
            oldLink = link;
        }
    }

    /**
     * Checks that the virtual ports (virtual link end-points) are mapped on the
     * same physical ports that delimit the physical path.
     *
     * @param tenantId the tenant ID
     * @param srcDpid the virtual source datapath ID
     * @param ovxSrcPort the source port number
     * @param dstDpid the virtual destination datapath ID
     * @param ovxDstPort the destination port number
     * @param physicalLinks the path
     * @throws NetworkMappingException
     * @throws VirtualLinkException
     */
    public static void areValidLinkEndPoints(final int tenantId,
            final long srcDpid, final short ovxSrcPort, final long dstDpid,
            final short ovxDstPort, final List<PhysicalLink> physicalLinks)
                    throws NetworkMappingException {
        OVXNetwork net = OVXMap.getInstance().getVirtualNetwork(tenantId);
        OVXPort srcPort = net.getSwitch(srcDpid).getPort(ovxSrcPort);
        OVXPort dstPort = net.getSwitch(dstDpid).getPort(ovxDstPort);
        if (!srcPort.getPhysicalPort()
                .equals(physicalLinks.get(0).getSrcPort())) {
            throw new VirtualLinkException(
                    "The virtual link source port and the physical path src port are"
                    + "not mapped on the same physical port. Virtual port is mapped on: "
                            + srcPort.getPhysicalPort().getParentSwitch()
                            .getSwitchName()
                            + "/"
                            + srcPort.getPhysicalPort().getPortNumber()
                            + ", physical path starts from: "
                            + physicalLinks.get(0).getSrcPort()
                            .getParentSwitch().getSwitchName()
                            + "/"
                            + physicalLinks.get(0).getSrcPort().getPortNumber());
        }
        if (!dstPort.getPhysicalPort().equals(
                physicalLinks.get(physicalLinks.size() - 1).getDstPort())) {
            throw new VirtualLinkException(
                    "The virtual link destination port and the physical path dst port are"
                    + "not mapped on the same physical port. Virtual port is mapped on: "
                            + dstPort.getPhysicalPort().getParentSwitch()
                            .getSwitchName()
                            + "/"
                            + dstPort.getPhysicalPort().getPortNumber()
                            + ", physical path starts from: "
                            + physicalLinks.get(physicalLinks.size() - 1)
                            .getDstPort().getParentSwitch()
                            .getSwitchName()
                            + "/"
                            + physicalLinks.get(physicalLinks.size() - 1)
                            .getDstPort().getPortNumber());
        }
    }

    /**
     * Gets the list of physical link instances based on the given
     * path string.
     *
     * @param pathString the path string
     * @return list of physical links
     */
    public static List<PhysicalLink> getPhysicalPath(String pathString) {
        final List<PhysicalLink> physicalLinks = new LinkedList<PhysicalLink>();
        for (final String hop : pathString.split(",")) {
            final String srcString = hop.split("-")[0];
            final String dstString = hop.split("-")[1];
            final String[] srcDpidPort = srcString.split("/");
            final String[] dstDpidPort = dstString.split("/");
            final long srcDpid = Long.parseLong(srcDpidPort[0]);
            final long dstDpid = Long.parseLong(dstDpidPort[0]);
            final PhysicalPort srcPort = PhysicalNetwork.getInstance()
                    .getSwitch(srcDpid).getPort(Short.valueOf(srcDpidPort[1]));
            final PhysicalPort dstPort = PhysicalNetwork.getInstance()
                    .getSwitch(dstDpid).getPort(Short.valueOf(dstDpidPort[1]));
            final PhysicalLink link = PhysicalNetwork.getInstance().getLink(
                    srcPort, dstPort);
            if (link == null) {
                throw new VirtualLinkException("Invalid physical hop: " + hop);
            }
            physicalLinks.add(link);
        }
        if (physicalLinks.size() == 0) {
            throw new VirtualLinkException(
                    "Need to specify a path of at least one hop lenght");
        }
        return physicalLinks;
    }
}
