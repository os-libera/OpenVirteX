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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;

import java.util.LinkedList;
import java.util.List;

public class OVXTableStatsRequest extends OVXStatistics implements DevirtualizableStatistic {

    protected OFTableStatsRequest ofTableStatsRequest;
    protected OFTableStatsEntry ofTableStatsEntry;

    int OFPFW_ALL = (1 << 22) - 1;
    int OFPFW_NW_DST_SHIFT = 14;
    int OFPFW_NW_DST_ALL = 32 << OFPFW_NW_DST_SHIFT;

    public OVXTableStatsRequest(OFMessage ofMessage) {
        super(OFStatsType.TABLE);

        this.ofTableStatsRequest = (OFTableStatsRequest)ofMessage;
    }

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw, final OVXStatisticsRequest msg) {
        OFFactory ofFactory = OFFactories.getFactory(msg.getOFMessage().getVersion());

        List<OFTableStatsEntry> tableStatsEntries = new LinkedList<OFTableStatsEntry>();

        if(ofFactory.getVersion() == OFVersion.OF_10) {

            this.ofTableStatsEntry = ofFactory.buildTableStatsEntry()
                    .setActiveCount(sw.getFlowTable().getFlowTable().size())
                    .setTableId(TableId.of(1))
                    .setWildcards(OFPFW_ALL & ~OFPFW_NW_DST_ALL & ~OFPFW_NW_DST_ALL)
                    .setName("Libera vFlowTable (incomplete)")
                    .setMaxEntries(100000)
                    .build();

            tableStatsEntries.add(this.ofTableStatsEntry);
        }else {

            this.ofTableStatsEntry = ofFactory.buildTableStatsEntry()
                    .setActiveCount(sw.getFlowTable().getFlowTable().size())
                    .setMatchedCount(U64.of(0))
                    .setLookupCount(U64.of(0))
                    .setTableId(TableId.of(1))
                    .build();

            tableStatsEntries.add(this.ofTableStatsEntry);
        }

        OVXStatisticsReply reply = new OVXStatisticsReply(
                ofFactory.buildTableStatsReply()
                        .setXid(msg.getOFMessage().getXid())
                        .setEntries(tableStatsEntries)
                        .build()
        );

        sw.sendMsg(reply, sw);
    }

    @Override
    public int hashCode() {
        return this.ofTableStatsEntry.hashCode();
    }
}
