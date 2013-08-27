package net.onrc.openvirtex.messages.actions;

import java.util.List;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;


public interface VirtualizableAction {
    
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OFMatch match) throws ActionVirtualizationDenied;
    

}
