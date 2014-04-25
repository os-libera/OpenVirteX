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

import net.onrc.openvirtex.core.OpenVirteX;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Virtual description statistics message handling.
 */
public class OVXDescriptionStatistics extends OFDescriptionStatistics implements
        VirtualizableStatistic, DevirtualizableStatistic {

    /**
     * Creates a reply object populated with the virtual switch params and
     * sends it back to the controller.
     * This is in response to receiving a Description stats request from the controller.
     *
     * @param sw the virtual switch
     * @param msg the statistics request message
     */
    @Override
    public void devirtualizeStatistic(final OVXSwitch sw,
            final OVXStatisticsRequest msg) {
        final OVXStatisticsReply reply = new OVXStatisticsReply();

        final OVXDescriptionStatistics desc = new OVXDescriptionStatistics();

        desc.setDatapathDescription(OVXSwitch.DPDESCSTRING);
        desc.setHardwareDescription("virtual hardware");
        desc.setManufacturerDescription("Open Networking Lab");
        desc.setSerialNumber(sw.getSwitchName());
        desc.setSoftwareDescription(OpenVirteX.VERSION);

        reply.setXid(msg.getXid());
        reply.setLengthU(reply.getLength() + desc.getLength());
        reply.setStatisticType(OFStatisticsType.DESC);
        reply.setStatistics(Collections.singletonList(desc));
        sw.sendMsg(reply, sw);

    }

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw,
            final OVXStatisticsReply msg) {
        // log.error("Received illegal message form physical network; {}", msg);

    }

}
