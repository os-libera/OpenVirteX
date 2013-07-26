package net.onrc.openvirtex.core.io;

import org.openflow.protocol.OFMessage;

public interface OVXSendMsg {
	public void sendMsg(OFMessage msg, OVXSendMsg from);
}
