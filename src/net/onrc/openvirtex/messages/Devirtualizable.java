package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;

public interface Devirtualizable {
    public void devirtualize(OVXSwitch sw);
}
