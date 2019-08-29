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

import net.onrc.openvirtex.core.OpenVirteX;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import org.projectfloodlight.openflow.protocol.*;

/**
 * Created by Administrator on 2016-06-14.
 */
public class OVXDescStatsRequest extends OVXStatistics implements DevirtualizableStatistic {
    protected OFDescStatsRequest ofDescStatsRequest;
    public OVXDescStatsRequest(OFMessage ofMessage) {
        super(OFStatsType.DESC);

        this.ofDescStatsRequest = (OFDescStatsRequest)ofMessage;
    }

    @Override
    public void devirtualizeStatistic(OVXSwitch sw, OVXStatisticsRequest msg) {

        OFDescStatsReply ofDescStatsReply = OFFactories.getFactory(msg.getOFMessage().getVersion()).buildDescStatsReply()
                .setDpDesc(OVXSwitch.DPDESCSTRING)
                .setHwDesc("Virtual Hardware")
                .setMfrDesc("Libera Team")
                .setSerialNum(sw.getSwitchName())
                .setSwDesc(OpenVirteX.VERSION)
                .setXid(msg.getOFMessage().getXid())
                .build();



        final OVXStatisticsReply reply = new OVXStatisticsReply(ofDescStatsReply);

        sw.sendMsg(reply, sw);
    }

    @Override
    public int hashCode() {
        return this.ofDescStatsRequest.hashCode();
    }
}
