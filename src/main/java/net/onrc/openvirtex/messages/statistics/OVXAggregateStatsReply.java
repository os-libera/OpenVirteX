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
import org.projectfloodlight.openflow.protocol.OFAggregateStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFStatsType;

public class OVXAggregateStatsReply extends OVXStatistics implements VirtualizableStatistic {

    protected OFAggregateStatsReply ofAggregateStatsReply;
    protected long packetCount;
    protected long byteCount;
    protected int flowCount;

    public OVXAggregateStatsReply(OFMessage ofMessage) {
        super(OFStatsType.AGGREGATE);
        this.ofAggregateStatsReply = (OFAggregateStatsReply)ofMessage;

        this.packetCount = this.ofAggregateStatsReply.getPacketCount().getValue();
        this.byteCount = this.ofAggregateStatsReply.getByteCount().getValue();
        this.flowCount = (int)this.ofAggregateStatsReply.getFlowCount();
    }

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw, final OVXStatisticsReply msg) {


    }

    @Override
    public int hashCode() {
        return this.ofAggregateStatsReply.hashCode();
    }
}
