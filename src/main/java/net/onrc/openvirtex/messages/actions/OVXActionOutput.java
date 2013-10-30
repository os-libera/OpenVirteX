/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.messages.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

public class OVXActionOutput extends OFActionOutput implements
VirtualizableAction {
	Logger log = LogManager.getLogger(OVXActionOutput.class.getName());

	@Override
	public void virtualize(final OVXSwitch sw,
			final List<OFAction> approvedActions, final OVXMatch match)
					throws ActionVirtualizationDenied, DroppedMessageException {
		final OVXPort inPort = sw.getPort(match.getInputPort());
		final LinkedList<OVXPort> outPortList = this.fillPortList(
				match.getInputPort(), this.getPort(), sw);
		final OVXNetwork vnet;
		try {
			vnet = sw.getMap().getVirtualNetwork(sw.getTenantId());
		} catch (NetworkMappingException e) {
			log.warn("{}: skipping processing of OFAction", e);
			return;
		}

		if (match.isFlowMod()) {
			/*
			 * FlowMod management
			 * Iterate through the output port list. Two main scenarios:
			 * 	- OVXSwitch is BigSwitch and inPort & outPort belongs to different physical switches
			 * 	- Other cases, e.g. SingleSwitch and BigSwitch with inPort & outPort belonging to the
			 * 		same physical switch
			 */

			// Retrieve the flowMod from the virtual flow map
			final OVXFlowMod fm = sw.getFlowMod(match.getCookie());
			fm.setCookie(match.getCookie());
			// TODO: Check if the FM has been retrieved

			for (final OVXPort outPort : outPortList) {
				Integer linkId = 0;
				Integer flowId = 0;

				/*
				 * OVXSwitch is BigSwitch and inPort & outPort belongs to different physical switches
				 */
				if (sw instanceof OVXBigSwitch
						&& inPort.getPhysicalPort().getParentSwitch() != outPort
						.getPhysicalPort().getParentSwitch()) {
					// This list includes all the actions that have to be applied at the end of the route
					final LinkedList<OFAction> outActions = new LinkedList<OFAction>();

					//Retrieve the route between the two OVXPorts
					final OVXBigSwitch bigSwitch = (OVXBigSwitch) outPort
							.getParentSwitch();
					final SwitchRoute route = bigSwitch.getRoute(inPort,
							outPort);
					if (route == null) {
						this.log.error(
								"Cannot retrieve the bigswitch internal route between ports {} {}, dropping message",
								inPort, outPort);
						return;
					}

					//If the inPort belongs to an OVXLink, add rewrite actions to unset the packet link fields
					if (inPort.isLink()) {
						final OVXPort dstPort = vnet.getNeighborPort(inPort);
						final OVXLink link = inPort.getLink().getOutLink();
						if (link != null) {
							flowId = vnet.getFlowManager().
									getFlowId(match.getDataLayerSource(), match.getDataLayerDestination());
							OVXLinkUtils lUtils = new OVXLinkUtils(sw.getTenantId(), link.getLinkId(), flowId);
							approvedActions.addAll(lUtils.unsetLinkFields());
						} else {
							this.log.error(
									"Cannot retrieve the virtual link between ports {} {}, dropping message",
									dstPort, inPort);
							return;
						}
					}

					/*
					 * Check the outPort:
					 * 	- if it's an edge, configure the route's last FM to rewrite the IPs 
					 * 		and generate the route FMs
					 * 	- if it's a link:
					 * 		- retrieve the link
					 * 		- generate the link FMs
					 * 		- configure the route's last FM to rewrite the MACs
					 * 		- generate the route FMs
					 */
					if (outPort.isEdge()) {
						outActions.addAll(this.prependUnRewriteActions(match));
						route.generateRouteFMs(fm, outActions, inPort, outPort);
					} else {
						final OVXLink link = outPort.getLink().getOutLink();
						linkId = link.getLinkId();
						try {
							flowId = vnet.getFlowManager()
									.storeFlowValues(match.getDataLayerSource(),
											match.getDataLayerDestination());
							link.generateLinkFMs(fm.clone(), flowId);
							outActions.addAll(new OVXLinkUtils(sw.getTenantId(), linkId, flowId).setLinkFields());
							route.generateRouteFMs(fm, outActions, inPort, outPort);
						} catch (IndexOutOfBoundException e) {
							log.error("Too many host to generate the flow pairs in this virtual network {}. "
									+ "Dropping flow-mod {} ", sw.getTenantId(), fm);
							throw new DroppedMessageException();
						}
					}
					//add the output action with the physical outPort (srcPort of the route)
					if (inPort.getPhysicalPortNumber() != route.getPathSrcPort().getPortNumber())
						approvedActions.add(new OFActionOutput(route.getPathSrcPort().getPortNumber()));
					else 
						approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));
				} 
				/*
				 * SingleSwitch and BigSwitch with inPort & outPort belonging 
				 * to the same physical switch
				 */
				else {
					if (inPort.isEdge()) {
						if (outPort.isEdge()) {
							//TODO: this is logically incorrect, remove and check if the system works 
							approvedActions.addAll(this
									.prependUnRewriteActions(match));
						} else {
							/*
							 * If inPort is edge and outPort is link:
							 * 	- retrieve link
							 * 	- generate the link's FMs
							 * 	- add actions to current FM to write packet fields related to the link 
							 */
							final OVXLink link = outPort.getLink().getOutLink();
							linkId = link.getLinkId();
							try {
								flowId = vnet.getFlowManager()
										.storeFlowValues(
												match.getDataLayerSource(),
												match.getDataLayerDestination());
								link.generateLinkFMs(fm.clone(), flowId);
								approvedActions.addAll(
										new OVXLinkUtils(sw.getTenantId(), linkId, flowId).setLinkFields());
							} catch (IndexOutOfBoundException e) {
								log.error("Too many host to generate the flow pairs in this virtual network {}. "
										+ "Dropping flow-mod {} ", sw.getTenantId(), fm);
								throw new DroppedMessageException();
							}
						}
					} else {
						if (outPort.isEdge()) {
							/*
							 * If inPort belongs to a link and outPort is edge:
							 * 	- retrieve link
							 * 	- add actions to current FM to restore original IPs
							 * 	- add actions to current FM to restore packet fields related to the link 
							 */
							approvedActions.addAll(this
									.prependUnRewriteActions(match));
							// rewrite the OFMatch with the values of the link
							final OVXPort dstPort = vnet.getNeighborPort(inPort);
							final OVXLink link = dstPort.getLink().getOutLink();
							if (link != null) {
								flowId = vnet.getFlowManager().
										getFlowId(match.getDataLayerSource(), match.getDataLayerDestination());
								OVXLinkUtils lUtils = new OVXLinkUtils(sw.getTenantId(), link.getLinkId(), flowId);
								approvedActions.addAll(lUtils.unsetLinkFields());
							} else {
								// TODO: substitute all the return with
								// exceptions
								this.log.error(
										"Cannot retrieve the virtual link between ports {} {}, dropping message",
										dstPort, inPort);
								return;
							}
						} else {
							final OVXLink link = outPort.getLink().getOutLink();
							linkId = link.getLinkId();
							try {
								flowId = vnet.getFlowManager()
										.storeFlowValues(
												match.getDataLayerSource(),
												match.getDataLayerDestination());
								link.generateLinkFMs(fm.clone(), flowId);
								approvedActions.addAll(new OVXLinkUtils(sw.getTenantId(), linkId, flowId).setLinkFields());
							} catch (IndexOutOfBoundException e) {
								log.error("Too many host to generate the flow pairs in this virtual network {}. "
										+ "Dropping flow-mod {} ", sw.getTenantId(), fm);
								throw new DroppedMessageException();
							}
						}
					}
					if (inPort.getPhysicalPortNumber() != outPort.getPhysicalPortNumber())
						approvedActions.add(new OFActionOutput(outPort.getPhysicalPortNumber()));
					else 
						approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));
				}
				// TODO: Check if I need to do the unrewrite here for the single
				// switch
			}
		} else
			if (match.isPacketOut()) {
				/*
				 * PacketOut management.
				 * Iterate through the output port list. Three possible scenarios:
				 * 	- outPort belongs to a link: send a packetIn coming from the 
				 * 		virtual link end point to the controller
				 * 	- outPort is an edge port: two different sub-cases:
				 * 		- inPort & outPort belongs to the same physical switch, e.g. rewrite outPort
				 * 		- inPort & outPort belongs to different switches (bigSwitch): send a packetOut to 
				 * 			the physical port @ the end of the BS route
				 */

				// TODO check how to delete the packetOut and if it's required
				boolean throwException = true;

				for (final OVXPort outPort : outPortList) {
					/**
					 * If the outPort belongs to a virtual link, generate a packetIn 
					 * coming from the end point of the link to the controller.
					 */
					if (outPort.isLink()) {
						final OVXPort dstPort = outPort.getLink().getOutLink().getDstPort();
						dstPort.getParentSwitch().sendMsg(
								new OVXPacketIn(match.getPktData(),
										dstPort.getPortNumber()), null);
						this.log.debug(
								"Generate a packetIn from OVX Port {}/{}, phisicalPort {}/{}",
								dstPort.getParentSwitch().getSwitchName(), dstPort.getPortNumber(), 
								dstPort.getPhysicalPort().getParentSwitch().getSwitchName(), 
								dstPort.getPhysicalPortNumber());
					}

					/**
					 * Big-switch management. Generate a packetOut to the physical outPort
					 */
					else if (sw instanceof OVXBigSwitch) {
						final PhysicalPort dstPort = outPort
								.getPhysicalPort();
						dstPort.getParentSwitch().sendMsg(
								new OVXPacketOut(match.getPktData(),
										OFPort.OFPP_NONE.getValue(),
										dstPort.getPortNumber()), null);
						this.log.debug(
								"PacketOut for a bigSwitch port, "
										+ "generate a packet from Physical Port {}/{}",
										dstPort.getParentSwitch().getSwitchName(), dstPort.getPortNumber());

					}
					else { 
						/**
						 * Else (e.g. the outPort is an edgePort in a single switch) modify the packet and send to the physical switch.
						 * 
						 */
						throwException = false;
						approvedActions.addAll(this
								.prependUnRewriteActions(match));
						approvedActions.add(new OFActionOutput(outPort
								.getPhysicalPortNumber()));
						this.log.debug("Physical ports are on the same physical switch, rewrite only outPort to {}", outPort
								.getPhysicalPortNumber());
					}
				}
				if (throwException == true)
					throw new DroppedMessageException();
			}

	}

	private LinkedList<OVXPort> fillPortList(final Short inPort,
			final Short outPort, final OVXSwitch sw) throws DroppedMessageException {
		final LinkedList<OVXPort> outPortList = new LinkedList<OVXPort>();
		if (U16.f(outPort) < U16.f(OFPort.OFPP_MAX.getValue())) {
			if (sw.getPort(outPort).isActive())
				outPortList.add(sw.getPort(outPort));
		} 
		else if (U16.f(outPort) == U16.f(OFPort.OFPP_FLOOD.getValue())) {
			final Map<Short, OVXPort> ports = sw.getPorts();
			for (final OVXPort port : ports.values()) {
				if (port.getPortNumber() != inPort && port.isActive())
					outPortList.add(port);
			}
		} 
		else if (U16.f(outPort) == U16.f(OFPort.OFPP_ALL.getValue())) {
			final Map<Short, OVXPort> ports = sw.getPorts();
			for (final OVXPort port : ports.values()) {
				if (port.isActive())
					outPortList.add(port);
			}
		} 
		else
			log.warn("Output port from controller currently not supported. Short = {}, Exadecimal = 0x{}" , 
					U16.f(outPort), Integer.toHexString(U16.f(outPort) & 0xffff));

		if (outPortList.size() < 1)
			throw new DroppedMessageException("No output ports defined; dropping");
		return outPortList;
	}

	private List<OFAction> prependUnRewriteActions(final OFMatch match) {
		final List<OFAction> actions = new LinkedList<OFAction>();
		if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
			final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
			srcAct.setNetworkAddress(match.getNetworkSource());
			actions.add(srcAct);
		}
		if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
			final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
			dstAct.setNetworkAddress(match.getNetworkDestination());
			actions.add(dstAct);
		}
		return actions;
	}
}
