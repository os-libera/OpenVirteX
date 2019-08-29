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
package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFStatsType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class OVXFlowStatsReply extends OVXStatistics implements VirtualizableStatistic {

    Logger log = LogManager.getLogger(OVXFlowStatsReply.class.getName());

    protected OFFlowStatsReply ofFlowStatsReply;
    protected OFFlowStatsEntry ofFlowStatsEntry;

    public OVXFlowStatsReply(OFMessage ofMessage) {
        super(OFStatsType.FLOW);
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
        this.ofFlowStatsEntry = null;
    }

    public OVXFlowStatsReply(OFMessage ofMessage, OFFlowStatsEntry ofFlowStatsEntry) {
        super(OFStatsType.FLOW);
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
        this.ofFlowStatsEntry = ofFlowStatsEntry;
    }

    public void setOFMessage(OFMessage ofMessage) {
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
    }

    public OFMessage getOFMessage() {
        return this.ofFlowStatsReply;
    }

    public OFFlowStatsEntry getOFFlowStatsEntry() {
        return this.ofFlowStatsEntry;
    }

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw, final OVXStatisticsReply msg) {
        this.log.debug("virtualizeStatistic");
        this.log.debug(msg.getOFMessage().toString());

        if (msg.getOFMessage().getXid() != 0) {
            sw.removeFlowMods(msg);
            return;
        }

        HashMap<Integer, List<OFFlowStatsEntry>> stats = new HashMap<Integer, List<OFFlowStatsEntry>>();

        OFFlowStatsReply ofFlowStatsReply = (OFFlowStatsReply)msg.getOFMessage();

        for (OFFlowStatsEntry stat : ofFlowStatsReply.getEntries()) {
             int tid = getTidFromCookie(stat.getCookie().getValue());
            addToStats(tid, stat, stats);
        }

        sw.setFlowStatistics(stats);
    }

    private void addToStats(int tid, OFFlowStatsEntry reply,
                            HashMap<Integer, List<OFFlowStatsEntry>> stats) {
        List<OFFlowStatsEntry> statsList = stats.get(tid);
        if (statsList == null) {
            statsList = new LinkedList<OFFlowStatsEntry>();
        }
        statsList.add(reply);
        stats.put(tid, statsList);
    }

    private int getTidFromCookie(long cookie) {
        return (int) (cookie >> 32);
    }

    @Override
    public int hashCode() {
        return this.ofFlowStatsReply.hashCode();
    }
}
