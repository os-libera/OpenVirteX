package net.onrc.openvirtex.elements.datapath;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFMessage;

import net.onrc.openvirtex.elements.datapath.role.RoleManager;
import net.onrc.openvirtex.elements.network.Dom0;
import net.onrc.openvirtex.elements.network.OVXNetwork;

public class Dom0Manager extends RoleManager {

    private final Dom0 dom0;

    public Dom0Manager(OVXNetwork dom0) {
        super();
        this.dom0 = (Dom0) dom0;
    }
    
    public void sendMsg(OFMessage msg, Channel c) {
        this.dom0.handleMsg(msg);
    }
    
}
