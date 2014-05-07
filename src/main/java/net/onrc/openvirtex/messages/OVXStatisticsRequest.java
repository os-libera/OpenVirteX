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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.statistics.DevirtualizableStatistic;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;
import net.onrc.openvirtex.messages.statistics.OVXTableStatistics;
import net.onrc.openvirtex.messages.statistics.OVXVendorStatistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class OVXStatisticsRequest extends OFStatisticsRequest implements
        Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXStatisticsRequest.class
            .getName());

    @Override
    public void devirtualize(final OVXSwitch sw) {
        switch (this.statisticType) {
        // Desc, vendor, table stats have no body. fuckers.
        case DESC:
            new OVXDescriptionStatistics().devirtualizeStatistic(sw, this);
            break;
        case TABLE:
            new OVXTableStatistics().devirtualizeStatistic(sw, this);
            break;
        case VENDOR:
            new OVXVendorStatistics().devirtualizeStatistic(sw, this);
            break;
        default:
            try {
                final OFStatistics stat = this.getStatistics().get(0);
                ((DevirtualizableStatistic) stat).devirtualizeStatistic(sw,
                        this);
            } catch (final ClassCastException e) {
                this.log.error("Statistic received is not devirtualizable {}",
                        this);
            }

        }

    }

}
