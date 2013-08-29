package net.onrc.openvirtex.api;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.ControllerUnavailableException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidLinkException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.VirtualLinkException;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class APITenantManager {

    Logger log = LogManager.getLogger(APITenantManager.class.getName());

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
     * @throws ControllerUnavailableException 
     */
    public Integer createOVXNetwork(final String protocol,
	    final String controllerAddress, final int controllerPort,
	    final String networkAddress, final short mask) throws ControllerUnavailableException {
	
	isControllerAvailable(controllerAddress, controllerPort);
	final IPAddress addr = new OVXIPAddress(networkAddress, -1); 
	final OVXNetwork virtualNetwork = new OVXNetwork(protocol, controllerAddress,
	        controllerPort, addr, mask);
	virtualNetwork.register();
	this.log.info("Created virtual network {}",
	        virtualNetwork.getTenantId());
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
     * @throws InvalidDPIDException 
     * @throws InvalidTenantIdException 
     */
    public long createOVXSwitch(final int tenantId, final List<String> dpids) throws InvalidDPIDException, InvalidTenantIdException {
	isValidTenantId(tenantId);
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final List<Long> longDpids = new ArrayList<Long>();
	for (final String dpid : dpids) {
	    final long longDpid = Long.parseLong(dpid);
	    longDpids.add(longDpid);
	}
	isValidDPID(tenantId, longDpids);
	final OVXSwitch ovxSwitch = virtualNetwork.createSwitch(longDpids);
	if (ovxSwitch == null) {
	    return -1;
	} else {
	    this.log.info("Created virtual switch {} in virtual network {}",
		    ovxSwitch.getSwitchId(), virtualNetwork.getTenantId());
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
     *            specify the physical dpid for which switch to attach the host
     *            to
     * @param port
     *            Specify which port on the virtualSwitch the host should be
     *            connected to
     * @return portNumber The portNumber is a short that represents the port of
     *         the edge switch which this edgePort is using
     * @throws InvalidTenantIdException 
     * @throws InvalidPortException 
     */
    public short createEdgePort(final int tenantId, final long dpid,
	    final short port, final String mac) throws InvalidTenantIdException, InvalidPortException {
	isValidTenantId(tenantId);
	
	// TODO: not sure if isValidPort is implemented properly ??????????
	isValidEdgePort(tenantId, dpid, port);
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final MACAddress macAddr = MACAddress.valueOf(mac);
	final OVXPort edgePort = virtualNetwork.createHost(dpid, port, macAddr);
	if (edgePort == null) {
	    return -1;
	} else {
	    this.log.info(
		    "Created edge port {} on virtual switch {} in virtual network {}",
		    edgePort.getPortNumber(), edgePort.getParentSwitch()
		            .getSwitchId(), virtualNetwork.getTenantId());
	    return edgePort.getPortNumber();
	}
    }

    /**
     * Takes a path of physicalLinks in a string and creates the virtualLink
     * based on this data. Each virtualLink consists of a set of PhysicalLinks
     * that are all continuous in the PhysicalNetwork topology.
     * 
     * @param tenantId
     *            Specify which virtualNetwork that the link is being created in
     * @param pathString
     *            The list of physicalLinks that make up the virtualLink
     * @return virtualLink the OVXLink object that is created using the
     *         PhysicalLinks
     * @throws VirtualLinkException 
     * @throws InvalidTenantIdException 
     * @throws InvalidLinkException 
     */
    public Integer createOVXLink(final int tenantId, final String pathString) throws VirtualLinkException, InvalidTenantIdException, InvalidLinkException {
	final List<PhysicalLink> physicalLinks = new LinkedList<PhysicalLink>();
	for (final String hop : pathString.split(",")) {
	    final String srcString = hop.split("-")[0];
	    final String dstString = hop.split("-")[1];
	    final String[] srcDpidPort = srcString.split("/");
	    final String[] dstDpidPort = dstString.split("/");
	    final PhysicalPort srcPort = PhysicalNetwork.getInstance()
		    .getSwitch(Long.valueOf(srcDpidPort[0]))
		    .getPort(Short.valueOf(srcDpidPort[1]));
	    final PhysicalPort dstPort = PhysicalNetwork.getInstance()
		    .getSwitch(Long.valueOf(dstDpidPort[0]))
		    .getPort(Short.valueOf(dstDpidPort[1]));
	    final PhysicalLink link = PhysicalNetwork.getInstance().getLink(
		    srcPort, dstPort);
	    physicalLinks.add(link);
	}
	isValidTenantId(tenantId);
	isVirtualLinkUnique(physicalLinks, pathString);
	
	// TODO: virtualLinkUnique should check if the physical topology allows for the path that has been specified
	//isValidLink(physicalLinks, pathString);
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	final OVXLink virtualLink = virtualNetwork.createLink(physicalLinks);
	if (virtualLink == null) {
	    return -1;
	} else {
	    this.log.info("Created virtual link {} in virtual network {}", virtualLink.getLinkId(),
		    virtualNetwork.getTenantId());
	    return virtualLink.getLinkId();
	}
    }

    /**
     * Creates and starts the network which is specified by the given
     * tenant id.
     * 
     * @param tenantId
     *            A unique Integer which identifies each virtual network
     * @throws InvalidTenantIdException 
     */
    public boolean bootNetwork(final int tenantId) throws InvalidTenantIdException {
	// initialize the virtualNetwork using the given tenantId
	isValidTenantId(tenantId);
	final OVXMap map = OVXMap.getInstance();
	final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId);
	this.log.info("Booted virtual network {}", virtualNetwork.getTenantId());
	return virtualNetwork.boot();
    }

    public String saveConfig() {
	// TODO Auto-generated method stub
	return null;
    }
    
    /**
     * Check that the tenant id specified refers to a virtual network in the system.
     * 
     * @param tenantId
     * @throws InvalidTenantIdException
     */
    public void isValidTenantId(int tenantId) throws InvalidTenantIdException {
	final OVXMap map = OVXMap.getInstance();
	if (map.getVirtualNetwork(tenantId) == null) {
	    throw new InvalidTenantIdException("the tenant id you have provided does not refer to a virtual network. tenant id: " + String.valueOf(tenantId));
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
    public void isValidDPID(int tenantId, List<Long> dpids) throws InvalidDPIDException {
	if (dpids.size() < 1){
	    throw new InvalidDPIDException("You did not provide any physical dpids. This must be provided in order to create a virtual switch");
	}
	PhysicalNetwork physicalNetwork = PhysicalNetwork.getInstance();
	// If any of the physical dpids that have been specified don't exist then we should throw an error
	for (long dpid:dpids) {
	    PhysicalSwitch sw = physicalNetwork.getSwitch(dpid);
	    if (sw == null) {
		throw new InvalidDPIDException("The physical dpids that you have provided are not accurate. dpid: " + String.valueOf(dpid));
	    }
	    
	    if (OVXMap.getInstance().getVirtualSwitch(sw, tenantId) != null) {
		throw new InvalidDPIDException("The physical dpid is already part of a virtual switch in the virtual network you have specified. dpid: " + String.valueOf(dpid));
	    }
	}
    }
    
    /**
     * Check that the virtual link we are trying to create is the only virtual link in the 
     * virtual network which has the same physical hops that have been specified.
     * 
     * @param virtualLink
     * @throws VirtualLinkException
     */
    public void isVirtualLinkUnique(List<PhysicalLink> physicalLinks, String pathString) throws VirtualLinkException {
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
		throw new VirtualLinkException("virtual link already exists. cannot create the same virtual link in the same virtual network. physical hops: " + pathString);
	    }
	}
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
    public void isControllerAvailable(String controllerAddress, int controllerPort) throws ControllerUnavailableException {
	for (OVXNetwork network : OVXMap.getInstance().getNetworkMap().values()) {
	    int port = network.getControllerPort();
	    String host = network.getControllerHost();
	    if (port == controllerPort && host.equals(controllerAddress)) {
		throw new ControllerUnavailableException("the controller we are trying to connect is already in use: " + String.valueOf(controllerPort) + " " + controllerAddress);
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
    public void isValidEdgePort(int tenantId, long dpid, short portNumber) throws InvalidPortException {
	PhysicalSwitch sw = PhysicalNetwork.getInstance().getSwitch(dpid);
	if (sw == null || sw.getPort(portNumber) == null || ! sw.getPort(portNumber).isEdge()) {
	    throw new InvalidPortException("The port specified is already being used: tenantId, dpid, port - " 
			+ String.valueOf(tenantId) + ", " + String.valueOf(dpid) 
			+ ", " + String.valueOf(portNumber));
	}
    }
    
    /**
     * Check to see if the physical path that has been provided is actually a valid
     * path in the physical topology.
     * 
     * @param links
     * @param pathString
     * @throws InvalidLinkException 
     */
    public void isValidLink(List <PhysicalLink> links, String pathString) throws InvalidLinkException {
	// TODO: implement the condition on when to throw this exception
	throw new InvalidLinkException("The physical links given are not actually connected in phsyical plane");
    }
}
