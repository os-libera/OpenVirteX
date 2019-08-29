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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.statistics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

public class OVXStatisticsRequest extends OVXMessage implements Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXStatisticsRequest.class.getName());

    private OVXStatistics statistics;

    public OVXStatisticsRequest(OFMessage msg) {
        super(msg);

        OFStatsRequest ofStatsRequest = (OFStatsRequest)msg;

        switch(ofStatsRequest.getStatsType())
        {
            case AGGREGATE:
                OVXAggregateStatsRequest ovxAggregateStatsRequest = new OVXAggregateStatsRequest(msg);
                this.statistics = ovxAggregateStatsRequest;
                break;
            case FLOW:
                OVXFlowStatsRequest ovxFlowStatsRequest = new OVXFlowStatsRequest(msg);
                this.statistics = ovxFlowStatsRequest;
                break;
            case PORT:
                OVXPortStatsRequest ovxPortStatsRequest = new OVXPortStatsRequest(msg);
                this.statistics = ovxPortStatsRequest;
                break;
            case QUEUE:
                OVXQueueStatsRequest ovxQueueStatsRequest =  new OVXQueueStatsRequest(msg);
                this.statistics = ovxQueueStatsRequest;
                break;
            case DESC:
                OVXDescStatsRequest ovxDescStatsRequest = new OVXDescStatsRequest(msg);
                this.statistics = ovxDescStatsRequest;
                break;
            case TABLE:
                OVXTableStatsRequest ovxTableStatsRequest = new OVXTableStatsRequest(msg);
                this.statistics = ovxTableStatsRequest;
                break;

            //for OFVersion13
            case PORT_DESC:
                OVXPortDescStatsRequest ovxPortDescStatsRequest = new OVXPortDescStatsRequest(msg);
                this.statistics = ovxPortDescStatsRequest;
                break;
            case METER:
                OVXMeterStatsRequest ovxMeterStatsRequest = new OVXMeterStatsRequest(msg);
                this.statistics = ovxMeterStatsRequest;
                break;
            //Unsupported so sending ERROR msg
            case GROUP:
                OVXGroupStatsRequest ovxGroupStatsRequest = new OVXGroupStatsRequest(msg);
                this.statistics = ovxGroupStatsRequest;
                break;
            case GROUP_DESC:
                OVXGroupDescStatsRequest ovxGroupDescStatsRequest = new OVXGroupDescStatsRequest(msg);
                this.statistics = ovxGroupDescStatsRequest;
                break;
            default:
                this.log.info("Not supporting StatsType " + ofStatsRequest.getStatsType());
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

    @Override
    public void devirtualize(final OVXSwitch sw) {
        //this.log.info("devirtualize");
        //this.log.info(this.getOFMessage().toString());
        try {
            final OVXStatistics stat = this.getStatistics();
            if(stat != null)
                ((DevirtualizableStatistic) stat).devirtualizeStatistic(sw, this);

        } catch (final ClassCastException e) {
            this.log.error("Statistic received is not devirtualizable {}",
                    this);
        }

    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
