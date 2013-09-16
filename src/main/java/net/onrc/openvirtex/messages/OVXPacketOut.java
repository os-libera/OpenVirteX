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

package net.onrc.openvirtex.messages;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

public class OVXPacketOut extends OFPacketOut implements Devirtualizable {

    private Logger log = LogManager.getLogger(OVXPacketOut.class.getName());
    private OFMatch match = null;
    private List<OFAction> approvedActions = new LinkedList<OFAction>();
    
    @Override
    public void devirtualize(OVXSwitch sw) {

	
	OVXPort inport = sw.getPort(this.getInPort());
	
	
	
	if (this.getBufferId() == OVXPacketOut.BUFFER_ID_NONE) {
	    if (this.getPacketData().length <= 14) {
		log.error("PacketOut has no buffer or data {}; dropping", this);
		sw.sendMsg(OVXMessageUtil.makeErrorMsg(OFBadRequestCode.OFPBRC_BAD_LEN, this), sw);
		return;
	    }
	    match = new OFMatch().loadFromPacket(this.packetData, this.inPort);
	} else {
	    OVXPacketIn cause = sw.getFromBufferMap(this.bufferId);
	    if (cause == null) {
		log.error("Unknown buffer id {} for virtual switch {}; dropping", this.bufferId, sw);
		return;
	    }
	
	    match = new OFMatch().loadFromPacket(cause.getPacketData(), this.inPort);
	    this.setBufferId(cause.getBufferId());           
	    if (cause.getBufferId() == OVXPacketOut.BUFFER_ID_NONE) {
                this.setPacketData(cause.getPacketData());
                this.setLengthU(this.getLengthU() + this.packetData.length);
            }
	}
	
	for (OFAction act : this.getActions()) {
	    try {
		((VirtualizableAction) act).virtualize(sw, approvedActions, match);
		
	    } catch (ActionVirtualizationDenied e) {
		log.warn("Action {} could not be virtualized; error: {}", act, e.getMessage());
		sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
		return;
	    } catch (DroppedMessageException e) {
                log.debug("Dropping flowmod {}", this);
                return;
            }
	}
	
	this.setInPort(inport.getPhysicalPortNumber());
	this.prependRewriteActions(sw);
	this.setActions(approvedActions);
	this.setActionsLength((short)0);
	this.setLengthU(OVXPacketOut.MINIMUM_LENGTH + this.packetData.length);
	for (final OFAction act : this.approvedActions) {
	    this.setLengthU(this.getLengthU() + act.getLengthU());
	    this.setActionsLength((short) (this.getActionsLength() + act.getLength()));
	}

	OVXMessageUtil.translateXid(this, inport);
        if (sw instanceof OVXBigSwitch) {
            ((OVXBigSwitch) sw).sendSouthBS(this, inport);
        } else {
            sw.sendSouth(this);
        }
    }
    
    
    private void prependRewriteActions(OVXSwitch sw) {
   	Mappable map = sw.getMap();
   	
   	if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
   	    OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(), 
   		    match.getNetworkSource());
   	    PhysicalIPAddress pip = map.getPhysicalIP(vip, sw.getTenantId());
   	    if (pip == null) {
   		pip = new PhysicalIPAddress(map.getVirtualNetwork(sw.getTenantId()).nextIP());
   		log.debug("Adding IP mapping {} -> {} for tenant {} at switch {}", vip, pip, 
   			sw.getTenantId(), sw.getName());
   		map.addIP(pip, vip);
   	    }
   	    OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
   	    srcAct.setNetworkAddress(pip.getIp());
   	    approvedActions.add(0,srcAct);
   	    
   	    
   	}

   	if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
   	    OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(), 
   		    match.getNetworkDestination());
   	    PhysicalIPAddress pip = map.getPhysicalIP(vip, sw.getTenantId());
   	    if (pip == null) {
   		pip = new PhysicalIPAddress(map.getVirtualNetwork(sw.getTenantId()).nextIP());
   		log.debug("Adding IP mapping {} -> {} for tenant {} at switch {}", vip, pip, 
   			sw.getTenantId(), sw.getName());
   		map.addIP(pip, vip);
   	    }
   	    OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
   	    dstAct.setNetworkAddress(pip.getIp());
   	 approvedActions.add(0, dstAct);
   	    	
   	   
   	}
       }
    
}
