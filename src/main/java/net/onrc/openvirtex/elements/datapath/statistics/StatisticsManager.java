package net.onrc.openvirtex.elements.datapath.statistics;


import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsRequest;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.statistics.OFStatisticsType;

public class StatisticsManager implements TimerTask, OVXSendMsg {

	
	
	private HashedWheelTimer timer = null;
	private PhysicalSwitch sw;

	public StatisticsManager(PhysicalSwitch sw) {
		this.timer = new HashedWheelTimer();
		this.sw = sw;
		
		timer.newTimeout(this, 1, TimeUnit.SECONDS);
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		sendPortStatistics();
		
		//TODO get value from cmd
		timeout.getTimer().newTimeout(this, 30, TimeUnit.SECONDS);
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
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}

	@Override
	public void sendMsg(OFMessage msg, OVXSendMsg from) {
		sw.sendMsg(msg, from);
	}

	@Override
	public String getName() {
		return "Statistics Manager (" + sw.getName() + ")";
	}


}
