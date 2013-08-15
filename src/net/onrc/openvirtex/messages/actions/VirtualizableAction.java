package net.onrc.openvirtex.messages.actions;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;


public interface VirtualizableAction {
    
    public boolean virtualize(OVXSwitch sw) throws ActionVirtualizationDenied;
    

}
