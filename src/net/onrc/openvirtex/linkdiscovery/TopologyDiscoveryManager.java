package net.onrc.openvirtex.linkdiscovery;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.messages.OVXMessageFactory;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

public class TopologyDiscoveryManager implements LLDPEventHandler {

    private static TopologyDiscoveryManager       instance;
    OVXMessageFactory                             ovxMessageFactory;
    private HashMap<Long, SwitchDiscoveryManager> switchDiscoveryManager;
    private final long                            updatePeriod;
    static long                                   defaultUpdatePeriod = 5000; // in
									      // milliseconds

    private TopologyDiscoveryManager() {
	this.updatePeriod = TopologyDiscoveryManager.defaultUpdatePeriod;
    }

    public static TopologyDiscoveryManager getInstance() {
	if (TopologyDiscoveryManager.instance == null) {
	    TopologyDiscoveryManager.instance = new TopologyDiscoveryManager();
	}
	return TopologyDiscoveryManager.instance;
    }

    public long getUpdatePeriod() {
	return this.updatePeriod;
    }

    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final SwitchDiscoveryManager switchDiscoveryManager = this.switchDiscoveryManager
	        .get(sw.getSwitchId());
	if (switchDiscoveryManager != null) {
	    switchDiscoveryManager.handleLLDP(msg, sw);
	}
    }

}
