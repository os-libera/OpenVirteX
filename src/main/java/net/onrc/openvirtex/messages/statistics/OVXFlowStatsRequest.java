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
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class OVXFlowStatsRequest extends OVXStatistics implements DevirtualizableStatistic {
    Logger log = LogManager.getLogger(OVXFlowStatsRequest.class.getName());

    protected OFFlowStatsRequest ofFlowStatsRequest;
    protected OFPort outPort;
    protected Match match;
    protected TableId tableId;

    public OVXFlowStatsRequest(OFMessage ofMessage) {
        super(OFStatsType.FLOW);
        this.ofFlowStatsRequest = (OFFlowStatsRequest)ofMessage;

        this.outPort = this.ofFlowStatsRequest.getOutPort();
        this.match = this.ofFlowStatsRequest.getMatch();
        this.tableId = this.ofFlowStatsRequest.getTableId();
    }

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw, final OVXStatisticsRequest msg) {
//        this.log.info("devirtualizeStatistic");

        List<OFFlowStatsEntry> replies = new LinkedList<OFFlowStatsEntry>();
        HashSet<Long> uniqueCookies = new HashSet<Long>();
        int tid = sw.getTenantId();

        if (this.outPort.getPortNumber() == OFPort.ANY.getPortNumber()) {
            for (PhysicalSwitch psw : getPhysicalSwitches(sw)) {
//                this.log.info("getTenantId " + tid);
                List<OFFlowStatsEntry> reps = psw.getFlowStats(tid);
                if (reps != null) {
//                    this.log.info("reps != null");

                    for (OFFlowStatsEntry stat : reps) {

//                        this.log.info(stat.toString());

//                        this.log.info("stat.getCookie() = " + stat.getCookie().toString());

                        if (!uniqueCookies.contains(stat.getCookie().getValue())) {
                            OVXFlowMod origFM;
                            try {
                                origFM = sw.getFlowMod(stat.getCookie().getValue());

                                uniqueCookies.add(stat.getCookie().getValue());
                            } catch (MappingException e) {
                                log.warn(
                                        "FlowMod not found in FlowTable for cookie={}, {}",
                                        stat.getCookie().toString(),
                                        stat.toString());
                                continue;
                            }

                            stat = stat.createBuilder()
                                    .setCookie(origFM.getFlowMod().getCookie())
                                    .setMatch(origFM.getFlowMod().getMatch())
                                    .build();

                            if(msg.getOFMessage().getVersion() == OFVersion.OF_10)
                                stat = stat.createBuilder().setActions(origFM.getFlowMod().getActions()).build();
                            else
                                stat = stat.createBuilder().setInstructions(origFM.getFlowMod().getInstructions()).build();


                            replies.add(stat);
                        }
                    }
                }
            }

            OFFlowStatsReply flowStatsReply = OFFactories.getFactory(sw.getOfVersion()).buildFlowStatsReply()
                    .setXid(msg.getOFMessage().getXid())
                    .setEntries(replies)
                    .build();

            OVXStatisticsReply reply = new OVXStatisticsReply(flowStatsReply);

            sw.sendMsg(reply, sw);
        }
    }

    private List<PhysicalSwitch> getPhysicalSwitches(OVXSwitch sw) {
        if (sw instanceof OVXSingleSwitch) {
            try {
                return sw.getMap().getPhysicalSwitches(sw);
            } catch (SwitchMappingException e) {
                log.debug("OVXSwitch {} does not map to any physical switches",
                        sw.getSwitchName());
                return new LinkedList<>();
            }
        }
        LinkedList<PhysicalSwitch> sws = new LinkedList<PhysicalSwitch>();
        for (OVXPort p : sw.getPorts().values()) {
            if (!sws.contains(p.getPhysicalPort().getParentSwitch())) {
                sws.add(p.getPhysicalPort().getParentSwitch());
            }
        }
        return sws;
    }

    @Override
    public int hashCode() {
        return this.ofFlowStatsRequest.hashCode();
    }
}
