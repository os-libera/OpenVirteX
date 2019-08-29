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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.statistics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

public class OVXStatisticsReply extends OVXMessage implements Virtualizable {

    private final Logger log = LogManager.getLogger(OVXStatisticsReply.class.getName());

    //OFStatsReply ofStatsReply;
    private OVXStatistics statistics;

    public OVXStatisticsReply(OFMessage msg) {
        super(msg);

        OFStatsReply ofStatsReply = (OFStatsReply)msg;

        //this.log.info(msg.toString());

        switch(ofStatsReply.getStatsType())
        {
            case AGGREGATE:
                OVXAggregateStatsReply ovxAggregateStatsReply = new OVXAggregateStatsReply(msg);
                this.statistics = ovxAggregateStatsReply;
                break;
            case DESC:
                //OVXDescriptionStatistics ovxDescriptionStatistics = new OVXDescriptionStatistics(msg);
                OVXDescStatsReply ovxDescStatsReply = new OVXDescStatsReply(msg);
                this.statistics = ovxDescStatsReply;
                break;
            case FLOW:
                OVXFlowStatsReply ovxFlowStatsReply = new OVXFlowStatsReply(msg);
                this.statistics = ovxFlowStatsReply;
                break;
            case PORT:
                OVXPortStatsReply ovxPortStatsReply = new OVXPortStatsReply(msg);
                this.statistics = ovxPortStatsReply;
                break;
            case QUEUE:
                OVXQueueStatsReply ovxQueueStatsReply =  new OVXQueueStatsReply(msg);
                this.statistics = ovxQueueStatsReply;
                break;
            case TABLE:
                OVXTableStatsReply ovxTableStatsReply = new OVXTableStatsReply(msg);
                this.statistics = ovxTableStatsReply;
                break;
            //for OFVersion13
            case PORT_DESC:
                OVXPortDescStatsReply ovxPortDescStatsReply = new OVXPortDescStatsReply(msg);
                this.statistics = ovxPortDescStatsReply;
                break;
            case METER:
                OVXMeterStatsReply ovxMeterStatsReply = new OVXMeterStatsReply(msg);
                this.statistics = ovxMeterStatsReply;
                break;
            default:
                this.log.info("Not supporting StatsType " + ofStatsReply.getStatsType());
                this.statistics = null;
                break;
        }
    }
    public void setStatistics(OVXStatistics statistics) {
        this.statistics = statistics;
    }

    public OVXStatistics getStatistics() {
        return this.statistics;
    }
/*
DESC FLOW AGGREGATE TABLE PORT QUEUE VENDOR
 */


    @Override
    public void virtualize(final PhysicalSwitch sw) {
        //this.log.info("virtualize ");
        //this.log.info(this.getOFMessage().toString());

        try {

            if (this.getStatistics() != null) {
                VirtualizableStatistic stat = (VirtualizableStatistic)this.getStatistics();
                stat.virtualizeStatistic(sw, this);
            } else if (this.getStatistics().getType() == OFStatsType.FLOW) {
                sw.setFlowStatistics(null);
            }

        } catch (final ClassCastException e) {
            this.log.error("Statistic received is not virtualizable {}", this);
        }
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
