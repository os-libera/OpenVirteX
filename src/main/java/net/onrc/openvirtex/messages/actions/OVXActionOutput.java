/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.messages.actions;



import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

public class OVXActionOutput extends OFActionOutput implements VirtualizableAction {
	Logger	log = LogManager.getLogger(OVXActionOutput.class.getName());
	
	@Override
	public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
			throws ActionVirtualizationDenied, DroppedMessageException {

		OVXPort inPort = sw.getPort(match.getInputPort());
		LinkedList<OVXPort> outPortList = fillPortList(match.getInputPort(), this.getPort(), sw);
		
		if (match.isFlowMod()) {
			//FlowMod management
			//Retrieve the flowMod from the virtual flow map
			
			//TODO: Ask Ayaka to return a copy of the FlowMod
			OVXFlowMod fm = sw.getFlowMod(match.getCookie());
			//TODO: Check if the FM has been retrieved
			for (OVXPort outPort : outPortList) { 
				Integer linkId = 0;
				Integer flowId = 0;
				
				if (sw instanceof OVXBigSwitch && 
						inPort.getPhysicalPort().getParentSwitch() != outPort.getPhysicalPort().getParentSwitch()) {
					LinkedList<OFAction> outActions = new LinkedList<OFAction>();
					
					PhysicalPort outPhyPort = outPort.getPhysicalPort();
					OVXBigSwitch bigSwitch = (OVXBigSwitch) outPort.getParentSwitch();
					SwitchRoute route = bigSwitch.getRoute(inPort, outPort);
					if (route == null) {
						log.error("Cannot retrieve the bigswitch internal route between ports {} {}, dropping message", inPort, outPort);
						return;
					}
					if (inPort.isLink()) {
						//rewrite the OFMatch with the values of the link
			    		OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(inPort);
						OVXLink link = sw.getMap().getVirtualNetwork(sw.getTenantId()).getLink(dstPort, inPort);
						if (link != null) {
							approvedActions.addAll(link.unsetLinkFields(match, sw));
						}
						else {
							log.error("Cannot retrieve the virtual link between ports {} {}, dropping message", dstPort, inPort);
							return;
						}
					}
					
					if (outPort.isEdge()) {
						outActions.addAll(prependUnRewriteActions(match));
						route.generateRouteFMs(fm, outActions, outPhyPort, sw);
					}
					else {
						OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(outPort);
						OVXLink link = sw.getMap().getVirtualNetwork(sw.getTenantId()).getLink(outPort, dstPort);
						linkId = link.getLinkId();
						flowId = sw.getMap().getVirtualNetwork(sw.getTenantId()).
								storeFlowValues(match.getDataLayerSource(), match.getDataLayerDestination());
						link.generateLinkFMs(fm, flowId, sw);
						outActions.addAll(link.setLinkFields(sw.getTenantId(), linkId, flowId));
						route.generateRouteFMs(fm, outActions, outPhyPort, sw);
					}
					approvedActions.add(new OFActionOutput(route.getRoute().get(0).getSrcPort().getPortNumber()));
				}
				else {
					if (inPort.isEdge()) {
						if (outPort.isEdge())
							approvedActions.addAll(prependUnRewriteActions(match));
						else {
							OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(outPort);
							OVXLink link = sw.getMap().getVirtualNetwork(sw.getTenantId()).getLink(outPort, dstPort);
							linkId = link.getLinkId();
							flowId = sw.getMap().getVirtualNetwork(sw.getTenantId())
									.storeFlowValues(match.getDataLayerSource(), match.getDataLayerDestination());
							link.generateLinkFMs(fm, flowId, sw);
							approvedActions.addAll(link.setLinkFields(sw.getTenantId(), linkId, flowId));
						}
					}
					else {
						if (outPort.isEdge()) {
							approvedActions.addAll(prependUnRewriteActions(match));
							//rewrite the OFMatch with the values of the link
				    		OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(inPort);
							OVXLink link = sw.getMap().getVirtualNetwork(sw.getTenantId()).getLink(dstPort, inPort);
							if (link != null)
								approvedActions.addAll(link.unsetLinkFields(match, sw));
							else {
								//TODO: substitute all the return with exceptions
								log.error("Cannot retrieve the virtual link between ports {} {}, dropping message", dstPort, inPort);
								return;
							}
						}
						else {
							OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(outPort);
							OVXLink link = sw.getMap().getVirtualNetwork(sw.getTenantId()).getLink(outPort, dstPort);
							linkId = link.getLinkId();
							flowId = sw.getMap().getVirtualNetwork(sw.getTenantId())
									.storeFlowValues(match.getDataLayerSource(), match.getDataLayerDestination());
							link.generateLinkFMs(fm, flowId, sw);
							approvedActions.addAll(link.setLinkFields(sw.getTenantId(), linkId, flowId));
						}
					}
					approvedActions.add(new OFActionOutput(outPort.getPhysicalPortNumber()));
				}
				//TODO: Check if I need to do the unrewrite here for the single switch
			}
		}
		else if (match.isPacketOut()) {
			//PacketOut management
			for (OVXPort outPort : outPortList) {
				if (outPort.isLink()) {
					OVXPort dstPort = sw.getMap().getVirtualNetwork(sw.getTenantId()).getNeighborPort(outPort);
					dstPort.getParentSwitch().sendMsg(createPacketIn(match.getPktData(), dstPort.getPortNumber()), null);
					this.log.debug("The outPort is of type Link, generate a packetIn from OVX Port {}", dstPort.getPortNumber());
					//TODO check if we have to remove the original packet out
				}
				else {
					if (sw instanceof OVXBigSwitch && 
							inPort.getPhysicalPort().getParentSwitch() != outPort.getPhysicalPort().getParentSwitch()) {
						OVXBigSwitch bigSwitch = (OVXBigSwitch) outPort.getParentSwitch();
						SwitchRoute route = bigSwitch.getRoute(inPort, outPort);
						PhysicalPort srcPort = route.getRoute().get(route.getRoute().size()-1).getDstPort();
						PhysicalPort dstPort = outPort.getPhysicalPort();
						//maybe I can put CONTROLLER as input port for packetOut
						dstPort.getParentSwitch().sendMsg(createPacketOut(match.getPktData(), srcPort.getPortNumber(), dstPort.getPortNumber()), null);
						//TODO check if we have to remove the original packet out
						this.log.debug("Physical ports are on different physical switches, "
								+ "generate a packetOut from Physical Port {}", dstPort.getPortNumber());
					}
					else {
						//TODO: maybe is better to check in the rewrite IPs stuff than do the actions two times
						approvedActions.addAll(prependUnRewriteActions(match));
						approvedActions.add(new OFActionOutput(outPort.getPhysicalPortNumber()));
						this.log.debug("Physical ports are on the same physical switch, rewrite only outPort");
					}
				}	
			}
		}

	}

	private LinkedList<OVXPort> fillPortList(Short inPort, Short outPort, OVXSwitch sw) {
		LinkedList<OVXPort> outPortList = new LinkedList<OVXPort>();
		if (U16.f(outPort) < U16.f(OFPort.OFPP_MAX.getValue())) {
			//Unicast
			outPortList.add(sw.getPort(outPort));
		}
		else if (U16.f(outPort) == U16.f(OFPort.OFPP_FLOOD.getValue())) {
			Map<Short, OVXPort> ports = sw.getPorts();
			for (OVXPort port : ports.values()) {
				if (port.getPortNumber() != inPort)
					outPortList.add(port);
			}
		}
		else {
			//TODO: Missing the OFPP_ALL case
		}
		
		return outPortList;
	}

	private List<OFAction> prependUnRewriteActions(final OFMatch match) {
		List<OFAction> actions = new LinkedList<OFAction>();
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
   
    private OVXPacketIn createPacketIn(byte[] data, short portNumber) { 
    	OVXPacketIn msg = new OVXPacketIn();
    	msg.setInPort(portNumber);
    	msg.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	msg.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
    	msg.setPacketData(data);
    	msg.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + msg.getPacketData().length));
    	msg.setLengthU(OFPacketIn.MINIMUM_LENGTH + msg.getPacketData().length);
    	return msg;
    }
    
	private OVXPacketOut createPacketOut(byte[] pktData, short inPort, short outPort) {
		OVXPacketOut msg = new OVXPacketOut();
		msg.setInPort(inPort);
		msg.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		OFActionOutput outAction = new OFActionOutput(outPort);
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		actions.add(outAction);
		msg.setActions(actions);
		msg.setActionsLength(outAction.getLength());
		msg.setPacketData(pktData);
		msg.setLengthU((short) (OFPacketOut.MINIMUM_LENGTH + msg.getPacketData().length + OFActionOutput.MINIMUM_LENGTH));
		return msg;
	}
	
}
