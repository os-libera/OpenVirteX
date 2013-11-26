package net.onrc.openvirtex.api.service.handlers.tenant;

import org.openflow.protocol.OFMessage;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

public class TestSwitch extends PhysicalSwitch {
	
	public TestSwitch(long dpid) {
		super(dpid);
	}
	
	@Override
	public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
		/* Hack to avoid NPE from not setting a channel for tests.
		 * Either this class or a dummy Channel implementation... */
	}	

}
