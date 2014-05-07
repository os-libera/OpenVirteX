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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.statistics.VirtualizableStatistic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFStatisticsType;

public class OVXStatisticsReply extends OFStatisticsReply implements
        Virtualizable {

    private final Logger log = LogManager.getLogger(OVXStatisticsReply.class
            .getName());

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        /*
         * The entire stat message will be handled in the specific stattype
         * handler.
         *
         * This means that for stattypes that have a list of replies the handles
         * will have to call getStatistics to handle them all.
         */
        try {

            if (this.getStatistics().size() > 0) {
                VirtualizableStatistic stat = (VirtualizableStatistic) this
                        .getStatistics().get(0);
                stat.virtualizeStatistic(sw, this);
            } else if (this.getStatisticType() == OFStatisticsType.FLOW) {
                sw.setFlowStatistics(null);
            }

        } catch (final ClassCastException e) {
            this.log.error("Statistic received is not virtualizable {}", this);
        }

    }

}
