package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

public interface Virtualizable {
	public void virtualize(PhysicalSwitch sw);
}
