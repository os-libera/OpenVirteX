package net.onrc.openvirtex.api;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.util.MACAddress;

public class APITenantManager {

    /**
     * Creates a new OVXNetwork object that is registered in the OVXMap. 
     * 
     * @param protocol
     * @param controllerAddress
     *            The IP address for the controller which controls this
     *            virtualNetwork
     * @param controllerPort
     *            The port which the controller and OVX will communicate on
     * @param networkAddress
     *            The IP address the virtual network uses
     * @param mask
     *            The IP range is defined using the mask
     * 
     * @return tenantId
     */
    public Integer createOVXNetwork(final String protocol,
	    final String controllerAddress, final int controllerPort,
	    final String networkAddress, final short mask) {
	final IPAddress addr = new OVXIPAddress(networkAddress, -1); 
	final OVXNetwork virtualNetwork = new OVXNetwork(protocol, controllerAddress,
	        controllerPort, addr, mask);
	virtualNetwork.register();
	return virtualNetwork.getTenantId();
    }

    /**
     * createOVXSwitch create a new switch object given a set of
     * physical dpid. This switch object will either be an OVXSwitch or
     * a OVXBigSwitch.
     * 
     * @param tenantId
     *            The tenantId will specify which virtual network the switch
     *            belongs to
     * @param dpids
     *            The list of physicalSwitch dpids to specify what the
     *            virtualSwitch is composed of
     * @return dpid Return the DPID of the virtualSwitch which we have just
     *         created
     */
    public long createOVXSwitch(final int tenantId, final List<String> dpids) {
	// TODO: check for min 1 element in dpids
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final List<Long> longDpids = new ArrayList<Long>();
	for (final String dpid : dpids) {
	    final long longDpid = Long.parseLong(dpid);
	    longDpids.add(longDpid);
	}
	OVXSwitch ovxSwitch = virtualNetwork.createSwitch(tenantId, longDpids);
	if (ovxSwitch == null) {
	    return -1;
	} else {
	    return ovxSwitch.getSwitchId();
	}
    }

    /**
     * To add a Host we have to create an edgePort which the host can connect
     * to.
     * So we create a new Port object and set the edge attribute to be True.
     * 
     * @param tenantId
     *            The tenantId is the integer to specify which virtualNetwork
     *            this host should be added to
     * @param dpid
     *            specify the virtual dpid for which switch to attach the host
     *            to
     * @param port
     *            Specify which port on the virtualSwitch the host should be
     *            connected to
     * @return portNumber The portNumber is a short that represents the port of
     *         the edge switch which this edgePort is using
     */
    public short createEdgePort(final int tenantId, final long dpid, final short port, final String mac) {
	final OVXMap map = OVXMap.getInstance();
	// TODO: check if tenantId exists
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final MACAddress macAddr = MACAddress.valueOf(mac);
	final OVXPort edgePort = virtualNetwork.createHost(dpid, port, macAddr);
	if (edgePort == null) {
	    return -1; 
	} else {
	    return edgePort.getPortNumber();
	}
    }

    /**
     * Takes a path of physicalLinks in a string and creates the virtualLink
     * based on this data. Each virtualLink consists of a set of PhysicalLinks
     * that are all continuous in the PHysicalNetwork topology.
     * 
     * @param tenantId
     *            Specify which virtualNetwork that the link is being created in
     * @param pathString
     *            The list of physicalLinks that make up the virtualLink
     * @return virtualLink the OVXLink object that is created using the
     *         PhysicalLinks
     */
    public Integer createOVXLink(final int tenantId, final String pathString) {
	List<PhysicalLink> physicalLinks = new LinkedList<PhysicalLink>();
	for (String hop:pathString.split(",")) {
	    String srcString = hop.split("-")[0];
	    String dstString = hop.split("-")[1];
	    String[] srcDpidPort = srcString.split("/");
	    String[] dstDpidPort = dstString.split("/");
	    PhysicalPort srcPort = PhysicalNetwork.getInstance().getSwitch(Long.valueOf(srcDpidPort[0])).getPort(Short.valueOf(srcDpidPort[1]));
	    PhysicalPort dstPort = PhysicalNetwork.getInstance().getSwitch(Long.valueOf(dstDpidPort[0])).getPort(Short.valueOf(dstDpidPort[1]));
	    PhysicalLink link = PhysicalNetwork.getInstance().getLink(srcPort, dstPort);
	    physicalLinks.add(link);
	}
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final OVXLink virtualLink = virtualNetwork.createLink(physicalLinks);
	if (virtualLink == null) {
	    return -1;
	} else {
	    return virtualLink.getLinkId();
	}
    }

    /**
     * Creates and starts the network which is specified by the given
     * tenant id.
     * 
     * @param tenantId
     *            A unique Integer which identifies each virtual network
     */
    public boolean bootNetwork(final int tenantId) {
	// initialize the virtualNetwork using the given tenantId
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	return virtualNetwork.boot();
    }
}
