package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.Switch;

public interface Devirtualizable {
    public void devirtualize(Switch sw);
}
