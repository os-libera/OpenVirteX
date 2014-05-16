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

import java.util.Collections;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;

public class OVXTableStatistics extends OFTableStatistics implements
        VirtualizableStatistic, DevirtualizableStatistic {

    /*
     * TODO Ideally, this would get information about the real flowtables and
     * aggregate them in some smart way. This probably needs to be discussed
     * with the overall OVX team
     */

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw,
            final OVXStatisticsRequest msg) {
        this.activeCount = sw.getFlowTable().getFlowTable().size();
        this.tableId = 1;
        /*
         * FIXME Currently preventing controllers from wildcarding the IP field.
         * That is if they actually look at this field.
         */
        this.wildcards = OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_ALL
                & ~OFMatch.OFPFW_NW_DST_ALL;
        this.name = "OVX vFlowTable (incomplete)";
        this.maximumEntries = 100000;
        OVXStatisticsReply reply = new OVXStatisticsReply();
        reply.setXid(msg.getXid());
        reply.setStatisticType(OFStatisticsType.TABLE);
        reply.setStatistics(Collections.singletonList(this));
        reply.setLengthU(OVXStatisticsReply.MINIMUM_LENGTH + this.getLength());
        sw.sendMsg(reply, sw);
    }

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw,
            final OVXStatisticsReply msg) {
        // TODO Auto-generated method stub

    }

}
