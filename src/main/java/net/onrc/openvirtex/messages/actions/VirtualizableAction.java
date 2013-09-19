package net.onrc.openvirtex.messages.actions;

import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;

import org.openflow.protocol.action.OFAction;

public interface VirtualizableAction {

	public void virtualize(OVXSwitch sw, List<OFAction> approvedActions,
			OVXMatch match) throws ActionVirtualizationDenied,
			DroppedMessageException;

}
