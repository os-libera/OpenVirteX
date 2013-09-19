package net.onrc.openvirtex.elements.link;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every fmRate milliseconds, loop over all fast ports and send an LLDP,
 * send an LLDP for a single slow port. Based on FlowVisor topology discovery
 * implementation.
 * 
 * TODO: add 'fast discovery' mode: drop LLDPs in destination switch but listen
 * for flow_removed messages
 */
public class OVXLinkManager implements OVXSendMsg, TimerTask {

	private final Integer tenantId;
	// send 1 fm every fmRate milliseconds
	private final long fmRate = 25000;
	private HashMap<OVXLink, HashMap<PhysicalSwitch, OVXFlowMod>> flowMap = null;
	private HashMap<OVXPort, HashMap<PhysicalSwitch, OVXFlowMod>> portFlowMap = null;
	private HashMap<SwitchRoute, HashMap<PhysicalSwitch, OVXFlowMod>> routeFlowMap = null;
	protected Mappable map = null;

	Logger log = LogManager.getLogger(OVXLinkManager.class.getName());

	public OVXLinkManager(final Integer tenantId) {
		this.tenantId = tenantId;
		this.flowMap = new HashMap<OVXLink, HashMap<PhysicalSwitch, OVXFlowMod>>();
		this.portFlowMap = new HashMap<OVXPort, HashMap<PhysicalSwitch, OVXFlowMod>>();
		this.routeFlowMap = new HashMap<SwitchRoute, HashMap<PhysicalSwitch, OVXFlowMod>>();
		this.map = OVXMap.getInstance();
		this.log.debug(
				"Initializing virtual link manager for virtual network {}",
				this.tenantId);
	}

	@Override
	public String getName() {
		return "OVXLinkManager for tenant " + this.tenantId;
	}

	public void registerOVXPort(final OVXPort ovxPort) {
		final HashMap<PhysicalSwitch, OVXFlowMod> portMap = new HashMap<PhysicalSwitch, OVXFlowMod>();
		final OFMatch match = new OFMatch();
		Wildcards wild = match.getWildcardObj();
		wild = wild.matchOn(Flag.DL_TYPE).matchOn(Flag.IN_PORT);
		match.setWildcards(wild.getInt());
		match.setInputPort(ovxPort.getPhysicalPortNumber());
		match.setDataLayerType(net.onrc.openvirtex.packet.Ethernet.TYPE_ARP);
		final OVXFlowMod fm = new OVXFlowMod();
		fm.setMatch(match);
		fm.setCommand(OFFlowMod.OFPFC_MODIFY);
		fm.setHardTimeout((short) 30);
		fm.setIdleTimeout((short) 0);
		fm.setPriority((short) 65535);
		fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		fm.setOutPort(OFPort.OFPP_NONE.getValue());

		fm.setActions(Arrays.asList((OFAction) new OFActionOutput(
				OFPort.OFPP_CONTROLLER.getValue(), (short) 0xffff)));
		fm.setLength((short) (OVXFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
		portMap.put(ovxPort.getPhysicalPort().getParentSwitch(), fm);
		System.out.println("resistro su questo switch e porta "
				+ ovxPort.getPhysicalPort().getParentSwitch().getName() + " "
				+ ovxPort.getPhysicalPortNumber() + " " + fm.toString());
		this.portFlowMap.put(ovxPort, portMap);
	}

	public boolean registerOVXLink(final OVXLink ovxLink) {
		Short inPort = 0;
		final HashMap<PhysicalSwitch, OVXFlowMod> linkMap = new HashMap<PhysicalSwitch, OVXFlowMod>();
		final int vNets = OpenVirteXController.getInstance()
				.getNumberVirtualNets();

		/*
		 * generate the flowMod, using the previous physical link dst port id as
		 * input port, and this physical link src port as output port
		 */
		final OFMatch match = new OFMatch();
		Wildcards wild = match.getWildcardObj();
		wild = wild.withNwDstMask(vNets).withNwSrcMask(vNets)
				.matchOn(Flag.DL_TYPE).matchOn(Flag.DL_VLAN)
				.matchOn(Flag.IN_PORT);
		match.setWildcards(wild.getInt());
		match.setInputPort(inPort);
		match.setDataLayerType(net.onrc.openvirtex.packet.Ethernet.TYPE_IPv4);
		match.setDataLayerVirtualLan(ovxLink.getLinkId().shortValue());
		// need to check with Ali how the tenantId will be splitted between the
		// addresses
		match.setNetworkSource(new PhysicalIPAddress(
				this.tenantId << 32 - vNets).getIp());
		match.setNetworkDestination(new PhysicalIPAddress(
				this.tenantId << 32 - vNets).getIp());

		for (final PhysicalLink phyLink : this.map.getPhysicalLinks(ovxLink)) {
			if (inPort != 0) {
				/*
				 * generate the flowMod, using the previous physical link dst
				 * port id as input port, and this physical link src port as
				 * output port
				 */
				final OFMatch curMatch = match.clone();
				curMatch.setInputPort(inPort);
				final OVXFlowMod fm = new OVXFlowMod();
				fm.setMatch(curMatch);
				fm.setCommand(OFFlowMod.OFPFC_MODIFY);
				fm.setHardTimeout((short) 30);
				fm.setIdleTimeout((short) 0);
				fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
				fm.setOutPort(OFPort.OFPP_NONE.getValue());

				fm.setActions(Arrays.asList((OFAction) new OFActionOutput(
						phyLink.getSrcPort().getPortNumber(), (short) 0xffff)));
				fm.setLength((short) (OVXFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));

				/*
				 * Save the FlowMod in the Map
				 */
				linkMap.put(phyLink.getSrcPort().getParentSwitch(), fm);

			}
			inPort = phyLink.getDstPort().getPortNumber();
		}
		if (!linkMap.isEmpty()) {
			this.flowMap.put(ovxLink, linkMap);
			return true;
		} else {
			return false;
		}
	}

	public boolean registerOVXRoute(final SwitchRoute route) {
		Short inPort = 0;
		final HashMap<PhysicalSwitch, OVXFlowMod> linkMap = new HashMap<PhysicalSwitch, OVXFlowMod>();
		final int vNets = OpenVirteXController.getInstance()
				.getNumberVirtualNets();

		/*
		 * generate the flowMod, using the previous physical link dst port id as
		 * input port, and this physical link src port as output port
		 */
		final OFMatch match = new OFMatch();
		Wildcards wild = match.getWildcardObj();
		wild = wild.withNwDstMask(vNets).withNwSrcMask(vNets)
				.matchOn(Flag.DL_TYPE).matchOn(Flag.DL_VLAN)
				.matchOn(Flag.IN_PORT);
		match.setWildcards(wild.getInt());
		match.setInputPort(inPort);
		match.setDataLayerType(net.onrc.openvirtex.packet.Ethernet.TYPE_IPv4);
		match.setDataLayerVirtualLan((short) route.getRouteId());
		// need to check with Ali how the tenantId will be splitted between the
		// addresses
		match.setNetworkSource(new PhysicalIPAddress(
				this.tenantId << 32 - vNets).getIp());
		match.setNetworkDestination(new PhysicalIPAddress(
				this.tenantId << 32 - vNets).getIp());

		for (final PhysicalLink phyLink : route.getRoute()) {
			if (inPort != 0) {
				/*
				 * generate the flowMod, using the previous physical link dst
				 * port id as input port, and this physical link src port as
				 * output port
				 */
				final OFMatch curMatch = match.clone();
				curMatch.setInputPort(inPort);
				final OVXFlowMod fm = new OVXFlowMod();
				fm.setMatch(curMatch);
				fm.setCommand(OFFlowMod.OFPFC_MODIFY);
				fm.setHardTimeout((short) 30);
				fm.setIdleTimeout((short) 0);
				fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
				fm.setOutPort(OFPort.OFPP_NONE.getValue());

				fm.setActions(Arrays.asList((OFAction) new OFActionOutput(
						phyLink.getSrcPort().getPortNumber(), (short) 0xffff)));
				fm.setLength((short) (OVXFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));

				/*
				 * Save the FlowMod in the Map
				 */
				linkMap.put(phyLink.getSrcPort().getParentSwitch(), fm);

			}
			inPort = phyLink.getDstPort().getPortNumber();
		}
		if (!linkMap.isEmpty()) {
			this.routeFlowMap.put(route, linkMap);
			return true;
		} else {
			return false;
		}
	}

	public boolean unregisterOVXLink(final OVXLink ovxLink) {
		final HashMap<PhysicalSwitch, OVXFlowMod> linkMap = this.flowMap
				.get(ovxLink);
		if (linkMap.isEmpty()) {
			return false;
		} else {
			for (final PhysicalSwitch phySwitch : linkMap.keySet()) {
				final OVXFlowMod fm = linkMap.get(phySwitch);
				fm.setCommand(OVXFlowMod.OFPFC_DELETE);
				fm.setFlags(OVXFlowMod.OFPFF_SEND_FLOW_REM); // verify if we
																// want the
																// flow-rem
																// message from
																// the switch
				// fm.setOutPort(port);
				phySwitch.sendMsg(fm, this);
			}

			this.flowMap.remove(ovxLink);
			return true;
		}

	}

	public void start() {
		PhysicalNetwork.getTimer().newTimeout(this, 10, TimeUnit.MILLISECONDS);
		this.log.debug("Starting virtual link manager for virtual network {}",
				this.tenantId);
	}

	/**
	 * Execute this method every fmRate milliseconds. Loops over all physical
	 * switches refreshing the proactive flow-mods that describe the virtual
	 * links
	 * 
	 * @param t
	 * @throws Exception
	 */
	@Override
	public void run(final Timeout t) {
		this.log.debug(
				"Sending virtual links proactive flow-mods for virtual network {}",
				this.tenantId);

		synchronized (this) {
			for (final OVXLink ovxLink : this.flowMap.keySet()) {
				for (final PhysicalSwitch phySwitch : this.flowMap.get(ovxLink)
						.keySet()) {
					this.log.trace("Sending flow-mod to sw {} , {}",
							phySwitch.getName(),
							this.flowMap.get(ovxLink).get(phySwitch).toString());
					phySwitch.sendMsg(this.flowMap.get(ovxLink).get(phySwitch),
							this);
				}
			}

			for (final OVXPort ovxPort : this.portFlowMap.keySet()) {
				for (final PhysicalSwitch phySwitch : this.portFlowMap.get(
						ovxPort).keySet()) {
					this.log.trace("Sending flow-mod to sw {} , {}",
							phySwitch.getName(), this.portFlowMap.get(ovxPort)
									.get(phySwitch).toString());
					phySwitch.sendMsg(
							this.portFlowMap.get(ovxPort).get(phySwitch), this);
				}
			}

			for (final SwitchRoute route : this.routeFlowMap.keySet()) {
				for (final PhysicalSwitch phySwitch : this.routeFlowMap.get(
						route).keySet()) {
					this.log.trace("Sending flow-mod to sw {} , {}",
							phySwitch.getName(), this.routeFlowMap.get(route)
									.get(phySwitch).toString());
					phySwitch.sendMsg(
							this.routeFlowMap.get(route).get(phySwitch), this);
				}
			}
		}

		// reschedule timer
		PhysicalNetwork.getTimer().newTimeout(this, this.fmRate,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	}

}
