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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.U16;

public class OVXFlowStatisticsRequest extends OFFlowStatisticsRequest implements
DevirtualizableStatistic {

    Logger log = LogManager.getLogger(OVXFlowStatisticsRequest.class.getName());

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw,
            final OVXStatisticsRequest msg) {
        final List<OVXFlowStatisticsReply> replies = new LinkedList<OVXFlowStatisticsReply>();
        final HashSet<Long> uniqueCookies = new HashSet<Long>();
        final int tid = sw.getTenantId();
        int length = 0;
        // the -1 is for beacon...
        if ((this.match.getWildcardObj().isFull() || this.match.getWildcards() == -1)
                && this.outPort == OFPort.OFPP_NONE.getValue()) {
            for (final PhysicalSwitch psw : this.getPhysicalSwitches(sw)) {
                final List<OVXFlowStatisticsReply> reps = psw.getFlowStats(tid);
                if (reps != null) {
                    for (final OVXFlowStatisticsReply stat : reps) {

                        if (!uniqueCookies.contains(stat.getCookie())) {
                            OVXFlowMod origFM;
                            try {
                                origFM = sw.getFlowMod(stat.getCookie());
                                uniqueCookies.add(stat.getCookie());
                            } catch (final MappingException e) {
                                this.log.warn(
                                        "FlowMod not found in FlowTable for cookie={}",
                                        stat.getCookie());
                                continue;
                            }
                            stat.setCookie(origFM.getCookie());
                            stat.setMatch(origFM.getMatch());
                            stat.setActions(origFM.getActions());
                            replies.add(stat);
                            stat.setLength(U16
                                    .t(OFFlowStatisticsReply.MINIMUM_LENGTH));
                            for (final OFAction act : stat.getActions()) {
                                stat.setLength(U16.t(stat.getLength()
                                        + act.getLength()));
                            }
                            length += stat.getLength();
                        }
                    }
                }
            }

            final OVXStatisticsReply reply = new OVXStatisticsReply();
            reply.setXid(msg.getXid());
            reply.setStatisticType(OFStatisticsType.FLOW);
            reply.setStatistics(replies);

            reply.setLengthU(OFStatisticsMessageBase.MINIMUM_LENGTH + length);

            sw.sendMsg(reply, sw);

        }
    }

    private List<PhysicalSwitch> getPhysicalSwitches(final OVXSwitch sw) {
        if (sw instanceof OVXSingleSwitch) {
            try {
                return sw.getMap().getPhysicalSwitches(sw);
            } catch (final SwitchMappingException e) {
                this.log.debug(
						"OVXSwitch {} does not map to any physical switches",
                        sw.getSwitchName());
                return new LinkedList<>();
            }
        }
        final LinkedList<PhysicalSwitch> sws = new LinkedList<PhysicalSwitch>();
        for (final OVXPort p : sw.getPorts().values()) {
            if (!sws.contains(p.getPhysicalPort().getParentSwitch())) {
                sws.add(p.getPhysicalPort().getParentSwitch());
            }
        }
        return sws;
    }

}
