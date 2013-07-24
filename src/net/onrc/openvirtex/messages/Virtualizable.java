package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.Switch;

public interface Virtualizable {
    public void virtualize(Switch sw);
}
