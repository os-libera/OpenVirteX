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
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

public class OVXFlowMod extends OFFlowMod implements Devirtualizable {

    private Logger log = LogManager.getLogger(OVXFlowMod.class.getName());

    private OVXSwitch sw = null;
    private List<OFAction> acts = new LinkedList<OFAction>();


    @Override
    public void devirtualize(OVXSwitch sw) {
	this.sw = sw;

	short inport = this.getMatch().getInputPort();

	for (OFAction act : this.getActions()) {
	    try {
		if (((VirtualizableAction) act).virtualize(sw)) {
		    prependRewriteActions();
		}
		acts.add(act);
	    } catch (ActionVirtualizationDenied e) {
		log.warn("Action {} could not be virtualized; error: {}", act, e.getMessage());
		//TODO: send error to controller
		return;
	    } 
	}

	OVXPort ovxInPort = sw.getPort(inport);
	this.getMatch().setInputPort(ovxInPort.getPhysicalPortNumber());
	if (ovxInPort.isEdge()) 
	    return;
	else 
	    rewriteMatch();
	
	this.setActions(acts);
	this.setLengthU(OVXFlowMod.MINIMUM_LENGTH);
	for (OFAction act : acts) 
	    this.setLengthU(this.getLengthU() + act.getLengthU());
	
	sw.sendSouth(this);
	
    }

    private void prependRewriteActions() {
	Mappable map = OVXMap.getInstance();
	
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
	    acts.add(srcAct);
	    
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
	    acts.add(dstAct);
	    

	}
    }

    private void rewriteMatch() {
	Mappable map = OVXMap.getInstance();

	//TODO: handle IP ranges
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
	    this.getMatch().setNetworkSource(pip.getIp()); 
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
	    this.getMatch().setNetworkDestination(pip.getIp());
	}

    }

    

}
