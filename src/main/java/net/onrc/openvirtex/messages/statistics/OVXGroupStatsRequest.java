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
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

public class OVXGroupStatsRequest extends OVXStatistics implements DevirtualizableStatistic {

    private Logger log = LogManager.getLogger(OVXGroupStatsRequest.class.getName());

    protected OFGroupStatsRequest ofGroupStatsRequest;
    public OVXGroupStatsRequest(OFMessage ofMessage) {
        super(OFStatsType.GROUP);

        this.ofGroupStatsRequest = (OFGroupStatsRequest)ofMessage;
    }

    @Override
    public void devirtualizeStatistic(OVXSwitch sw, OVXStatisticsRequest msg) {
        //This Request message is not supported. (FeaturesReply (Group is unflagged)
        //Send BadRequest Message

        sw.sendMsg(OVXMessageUtil.makeErrorMsg(OFBadRequestCode.BAD_TYPE, msg), sw);
    }
}
