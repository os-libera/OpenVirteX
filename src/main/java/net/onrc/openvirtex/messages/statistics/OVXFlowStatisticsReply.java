/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.messages.statistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;

import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXFlowStatisticsReply extends OFFlowStatisticsReply implements
        VirtualizableStatistic {

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw,
            final OVXStatisticsReply msg) {
        if (msg.getXid() != 0) {
            sw.removeFlowMods(msg);
            return;
        }

        HashMap<Integer, List<OVXFlowStatisticsReply>> stats = new HashMap<Integer, List<OVXFlowStatisticsReply>>();

        for (OFStatistics stat : msg.getStatistics()) {
            OVXFlowStatisticsReply reply = (OVXFlowStatisticsReply) stat;
            int tid = getTidFromCookie(reply.getCookie());
            addToStats(tid, reply, stats);
        }
        sw.setFlowStatistics(stats);
    }

    private void addToStats(int tid, OVXFlowStatisticsReply reply,
            HashMap<Integer, List<OVXFlowStatisticsReply>> stats) {
        List<OVXFlowStatisticsReply> statsList = stats.get(tid);
        if (statsList == null) {
            statsList = new LinkedList<OVXFlowStatisticsReply>();
        }
        statsList.add(reply);
        stats.put(tid, statsList);
    }

    private int getTidFromCookie(long cookie) {
        return (int) (cookie >> 32);
    }

}
