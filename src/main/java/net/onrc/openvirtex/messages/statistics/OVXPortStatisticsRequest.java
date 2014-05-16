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

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Implementation of virtual port statistics request.
 */
public class OVXPortStatisticsRequest extends OFPortStatisticsRequest implements
        DevirtualizableStatistic {

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw,
            final OVXStatisticsRequest msg) {
        List<OVXPortStatisticsReply> replies = new LinkedList<OVXPortStatisticsReply>();
        int length = 0;
        if (this.portNumber == OFPort.OFPP_NONE.getValue()) {
            for (OVXPort p : sw.getPorts().values()) {
                OVXPortStatisticsReply reply = p.getPhysicalPort()
                        .getParentSwitch()
                        .getPortStat(p.getPhysicalPort().getPortNumber());
                if (reply != null) {
                    /*
                     * Setting it here will also update the reference but this
                     * should not matter since we index our port stats struct by
                     * physical port number (so this info is not lost) and we
                     * always rewrite the port num to the virtual port number.
                     */
                    reply.setPortNumber(p.getPortNumber());
                    replies.add(reply);
                    length += reply.getLength();
                }
            }
            OVXStatisticsReply rep = new OVXStatisticsReply();
            rep.setStatisticType(OFStatisticsType.PORT);
            rep.setStatistics(replies);
            rep.setXid(msg.getXid());
            rep.setLengthU(OVXStatisticsReply.MINIMUM_LENGTH + length);
            sw.sendMsg(rep, sw);
        }
    }
}
