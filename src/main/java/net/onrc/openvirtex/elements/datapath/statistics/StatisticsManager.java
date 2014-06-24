/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath.statistics;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsRequest;
import net.onrc.openvirtex.protocol.OVXMatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.statistics.OFStatisticsType;

public class StatisticsManager implements TimerTask, OVXSendMsg {

    private HashedWheelTimer     timer           = null;
    private final PhysicalSwitch sw;

    Logger                       log             = LogManager
                                                         .getLogger(StatisticsManager.class
                                                                 .getName());

    private Integer              refreshInterval = 30;
    private boolean              stopTimer       = false;

    public StatisticsManager(final PhysicalSwitch sw) {
        /*
         * Get the timer from the PhysicalNetwork class.
         */
        this.timer = PhysicalNetwork.getTimer();
        this.sw = sw;
        this.refreshInterval = OpenVirteXController.getInstance()
                .getStatsRefresh();
    }

    @Override
    public void run(final Timeout timeout) throws Exception {
        this.log.debug("Collecting stats for {}", this.sw.getSwitchName());
        this.sendPortStatistics();
        this.sendFlowStatistics(0, (short) 0);

        if (!this.stopTimer) {
            this.log.debug("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getSwitchName());
            timeout.getTimer().newTimeout(this, this.refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void sendFlowStatistics(final int tid, final short port) {
        final OVXStatisticsRequest req = new OVXStatisticsRequest();
        // TODO: stuff like below should be wrapped into an XIDUtil class
        final int xid = tid << 16 | port;
        req.setXid(xid);
        req.setStatisticType(OFStatisticsType.FLOW);
        final OVXFlowStatisticsRequest freq = new OVXFlowStatisticsRequest();
        final OVXMatch match = new OVXMatch();
        match.setWildcards(Wildcards.FULL);
        freq.setMatch(match);
        freq.setOutPort(OFPort.OFPP_NONE.getValue());
        freq.setTableId((byte) 0xFF);
        req.setStatistics(Collections.singletonList(freq));
        req.setLengthU(req.getLengthU() + freq.getLength());
        this.sendMsg(req, this);
    }

    private void sendPortStatistics() {
        final OVXStatisticsRequest req = new OVXStatisticsRequest();
        req.setStatisticType(OFStatisticsType.PORT);
        final OVXPortStatisticsRequest preq = new OVXPortStatisticsRequest();
        preq.setPortNumber(OFPort.OFPP_NONE.getValue());
        req.setStatistics(Collections.singletonList(preq));
        req.setLengthU(req.getLengthU() + preq.getLength());
        this.sendMsg(req, this);
    }

    public void start() {

        /*
         * Initially start polling quickly. Then drop down to configured value
         */
        this.log.info("Starting Stats collection thread for {}",
                this.sw.getSwitchName());
        this.timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        this.log.info("Stopping Stats collection thread for {}",
                this.sw.getSwitchName());
        this.stopTimer = true;
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        this.sw.sendMsg(msg, from);
    }

    @Override
    public String getName() {
        return "Statistics Manager (" + this.sw.getName() + ")";
    }

    public void cleanUpTenant(final Integer tenantId, final short port) {
        this.sendFlowStatistics(tenantId, port);
    }

}
