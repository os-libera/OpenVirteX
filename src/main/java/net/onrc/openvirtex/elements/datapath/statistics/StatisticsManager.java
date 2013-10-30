package net.onrc.openvirtex.elements.datapath.statistics;


import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsRequest;
import net.onrc.openvirtex.protocol.OVXMatch;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.statistics.OFStatisticsType;

public class StatisticsManager implements TimerTask, OVXSendMsg {

	
	
	private HashedWheelTimer timer = null;
	private PhysicalSwitch sw;

	public StatisticsManager(PhysicalSwitch sw) {
		this.timer = PhysicalNetwork.getTimer();
		this.sw = sw;
		
		/*
		 * Initially start polling quickly.
		 * Then drop down to configured value
		 */
		timer.newTimeout(this, 1, TimeUnit.SECONDS);
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		sendPortStatistics();
		sendFlowStatistics(0, (short) 0);
		//TODO get value from cmd
		timeout.getTimer().newTimeout(this, 30, TimeUnit.SECONDS);
	}

	private void sendFlowStatistics(int tid, short port) {
		OVXStatisticsRequest req = new OVXStatisticsRequest();
		// TODO: stuff like below whould be wrapped into an XIDUtil class
		int xid = (tid << 16) | port; 
		req.setXid(xid);
		req.setStatisticType(OFStatisticsType.FLOW);
		OVXFlowStatisticsRequest freq = new OVXFlowStatisticsRequest();
		OVXMatch match = new OVXMatch();
		match.setWildcards(Wildcards.FULL);
		freq.setMatch(match);
		freq.setOutPort(OFPort.OFPP_NONE.getValue());
		freq.setTableId((byte)0xFF);
		req.setStatistics(Collections.singletonList(freq));
		req.setLengthU(req.getLengthU() + freq.getLength());
		sendMsg(req,this);
	}

	private void sendPortStatistics() {
		OVXStatisticsRequest req = new OVXStatisticsRequest();
		req.setStatisticType(OFStatisticsType.PORT);
		OVXPortStatisticsRequest preq = new OVXPortStatisticsRequest();
		preq.setPortNumber(OFPort.OFPP_NONE.getValue());
		req.setStatistics(Collections.singletonList(preq));
		req.setLengthU(req.getLengthU() + preq.getLength());
		sendMsg(req, this);
	}
	
	public void start() {
		
	}
	
	public void stop() {
		
	}

	@Override
	public void sendMsg(OFMessage msg, OVXSendMsg from) {
		sw.sendMsg(msg, from);
	}

	@Override
	public String getName() {
		return "Statistics Manager (" + sw.getName() + ")";
	}

	public void cleanUpTenant(Integer tenantId, short port) {
		sendFlowStatistics(tenantId, port);
	}


}
