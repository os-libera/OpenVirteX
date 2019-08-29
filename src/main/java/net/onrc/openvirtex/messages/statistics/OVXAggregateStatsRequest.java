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

import net.onrc.openvirtex.elements.datapath.FlowTable;
import net.onrc.openvirtex.elements.datapath.OVXSingleSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.messages.OVXStatisticsRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import java.util.*;

public class OVXAggregateStatsRequest extends OVXStatistics implements DevirtualizableStatistic {
    private Logger log = LogManager.getLogger(OVXAggregateStatsRequest.class.getName());

    protected OFAggregateStatsRequest ofAggregateStatsRequest;
    protected Match match;
    protected byte tableId;
    protected short outPort;

    public OVXAggregateStatsRequest(OFMessage ofMessage) {

        super(OFStatsType.AGGREGATE);
        this.ofAggregateStatsRequest = (OFAggregateStatsRequest)ofMessage;

        this.match = this.ofAggregateStatsRequest.getMatch();
        this.tableId = (byte)this.ofAggregateStatsRequest.getTableId().getValue();
        this.outPort = this.ofAggregateStatsRequest.getOutPort().getShortPortNumber();
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Match getMatch() {
        return this.match;
    }

    @Override
    public void devirtualizeStatistic(final OVXSwitch sw, final OVXStatisticsRequest msg) {

        int tid = sw.getTenantId();
        HashSet<Long> uniqueCookies = new HashSet<Long>();


        OFFactory ofFactory = OFFactories.getFactory(msg.getOFMessage().getVersion());

        Set<MatchField> matchFields = new HashSet<MatchField>();
        for(MatchField<?> field :  this.match.getMatchFields())
            matchFields.add(field);

        OFAggregateStatsReply ofAggregateStatsReply = ofFactory.buildAggregateStatsReply().build();

        if(matchFields.size() == 0 && this.outPort == OFPort.ANY.getPortNumber()) {
            FlowTable ft = sw.getFlowTable();
            ofAggregateStatsReply= ofAggregateStatsReply.createBuilder()
                                            .setFlowCount(ft.getFlowTable().size())
                                            .setByteCount(U64.of(0))
                                            .setPacketCount(U64.of(0))
                                            .build();

            for (PhysicalSwitch psw : getPhysicalSwitches(sw)) {
                List<OFFlowStatsEntry> reps = psw.getFlowStats(tid);
                if (reps != null) {
                    for (OFFlowStatsEntry s : reps) {

                        if (!uniqueCookies.contains(s.getCookie())) {
                            ofAggregateStatsReply = ofAggregateStatsReply.createBuilder()
                                    .setByteCount(U64.of(ofAggregateStatsReply.getByteCount().getValue() +
                                            s.getByteCount().getValue()))
                                    .setPacketCount(U64.of(ofAggregateStatsReply.getPacketCount().getValue() +
                                            s.getPacketCount().getValue()))
                                    .build();

                            uniqueCookies.add(s.getCookie().getValue());
                        }
                    }
                }
            }
        }

        OVXStatisticsReply reply =
                new OVXStatisticsReply(ofAggregateStatsReply.createBuilder()
                        .setXid(msg.getOFMessage().getXid())
                        .build());

        sw.sendMsg(reply, sw);

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
        return this.ofAggregateStatsRequest.hashCode();
    }
}
