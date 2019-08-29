/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.elements.datapath.statistics;

import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;

import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;


public class StatisticsManager implements TimerTask, OVXSendMsg {

    private HashedWheelTimer timer = null;
    private PhysicalSwitch sw;

    Logger log = LogManager.getLogger(StatisticsManager.class.getName());

    private Integer refreshInterval = 30;
    private boolean stopTimer = false;

    OFFactory ofFactory;

    public StatisticsManager(PhysicalSwitch sw) {
        /*
         * Get the timer from the PhysicalNetwork class.
         */
        this.timer = PhysicalNetwork.getTimer();
        this.sw = sw;
        this.refreshInterval = OpenVirteXController.getInstance()
                .getStatsRefresh();

        this.ofFactory = OFFactories.getFactory(sw.getOfVersion());
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        log.info("Collecting stats for {}", this.sw.getSwitchName());
        sendPortStatistics();
        sendFlowStatistics(0, (short) 0);

        if (!this.stopTimer) {
            log.info("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getSwitchName());
            timeout.getTimer().newTimeout(this, refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void sendFlowStatistics(int tid, short port) {
        int xid = (tid << 16) | port;

        OFFlowStatsRequest ofFlowStatsRequest = this.ofFactory.buildFlowStatsRequest()
                .setXid(xid)
                .setMatch(this.ofFactory.matchWildcardAll())
                .setOutPort(OFPort.ANY)
                .setTableId(TableId.ALL)
                .build();

        OVXStatisticsRequest req = new OVXStatisticsRequest(ofFlowStatsRequest);

        sendMsg(req, this);
    }

    private void sendPortStatistics() {
        // xid 설정 안하나?
        OFPortStatsRequest ofPortStatsRequest = this.ofFactory.buildPortStatsRequest()
                .setPortNo(OFPort.ANY)
                .build();

        OVXStatisticsRequest req = new OVXStatisticsRequest(ofPortStatsRequest);

        sendMsg(req, this);
    }

    public void start() {

        /*
         * Initially start polling quickly. Then drop down to configured value
         */
        log.info("Starting Stats collection thread for {}",
                this.sw.getSwitchName());
        timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        log.info("Stopping Stats collection thread for {}",
                this.sw.getSwitchName());
        this.stopTimer = true;
    }

    @Override
    public void sendMsg(OVXMessage msg, OVXSendMsg from) {
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
