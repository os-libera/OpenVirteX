package net.onrc.openvirtex.linkdiscovery;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.util.StringByteSerializer;

import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.messages.OVXMessageFactory;

public class LinkDiscoveryManager implements OVXSendMsg {

    private static LinkDiscoveryManager instance;
    OVXMessageFactory ovxMessageFactory;
    
    private LinkDiscoveryManager() {
    }

    public LinkDiscoveryManager getInstance() {
	if (instance == null) {
	    LinkDiscoveryManager.instance = new LinkDiscoveryManager();
	}
	return LinkDiscoveryManager.instance;
    }
        
    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	PhysicalPort port;
	OFPacketOut packetOut = (OFPacketOut) this.ovxMessageFactory
		.getMessage(OFType.PACKET_OUT);
	packetOut.setBufferId(-1);
	List<OFAction> actionsList = new LinkedList<OFAction>();
	OFActionOutput out = (OFActionOutput) this.ovxMessageFactory
		.getAction(OFActionType.OUTPUT);
	out.setPort(port.getPortNumber());
	actionsList.add(out);
	packetOut.setActions(actionsList);
	short alen = LinkDiscoveryManager.countActionsLen(actionsList);
	byte[] lldp = makeLLDP(port.getPortNumber(), port.getHardwareAddress());
	packetOut.setActionsLength(alen);
	packetOut.setPacketData(lldp);
	packetOut.setLength((short) (OFPacketOut.MINIMUM_LENGTH + alen + lldp.length));
	//sw.sendMsg(packetOut, this)
    }

    public static short countActionsLen(List<OFAction> actionsList) {
	short count = 0;
	for (OFAction act : actionsList)
	    count += act.getLength();
	return count;
    }

}
