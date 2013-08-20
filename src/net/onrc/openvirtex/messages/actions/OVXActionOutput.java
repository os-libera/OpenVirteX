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



import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

public class OVXActionOutput extends OFActionOutput implements VirtualizableAction {

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OFMatch match)
	    throws ActionVirtualizationDenied {

	int outport = U16.f(this.getPort());


	/*
	 * If we have a flood or all action then expand the 
	 * action list to include all the ports on the virtual 
	 * switch (minus the inport if it's a flood)
	 */
	if (outport == U16.f(OFPort.OFPP_ALL.getValue()) || 
		outport == U16.f(OFPort.OFPP_FLOOD.getValue())) {
	    Map<Short, OVXPort> ports = sw.getPorts();
	    for (OVXPort port : ports.values()) {
		if (port.getPortNumber() != match.getInputPort()) {
		    if (port.isEdge())
			prependUnRewriteActions(approvedActions, match);
		    approvedActions.add(new OFActionOutput(port.getPhysicalPortNumber()));
		}
	    }

	    if (outport == U16.f(OFPort.OFPP_ALL.getValue())) 
		approvedActions.add(new OFActionOutput(OFPort.OFPP_IN_PORT.getValue()));


	} else if (outport < U16.f(OFPort.OFPP_MAX.getValue())) {
	    OVXPort ovxPort = sw.getPort(this.getPort());
	    if (ovxPort != null) {
		if (ovxPort.isEdge()) 
		    prependUnRewriteActions(approvedActions, match);
		this.setPort(ovxPort.getPhysicalPortNumber());
	    } else
		throw new ActionVirtualizationDenied("Virtual Port " + this.getPort() + 
			" does not exist in virtual switch " + sw.getName());
	    approvedActions.add(this);
	} else
	    approvedActions.add(this);
	
    }

    private void prependUnRewriteActions(List<OFAction> approvedActions, final OFMatch match) {
	if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
	    final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
	    srcAct.setNetworkAddress(match.getNetworkSource());
	    approvedActions.add(srcAct);
	}
	if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
	    final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
	    dstAct.setNetworkAddress(match.getNetworkDestination());
	    approvedActions.add(dstAct);
	}
    }

}
