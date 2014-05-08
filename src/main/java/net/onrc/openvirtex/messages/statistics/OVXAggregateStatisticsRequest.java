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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
import org.openflow.protocol.OFPort;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFStatisticsType;

public class OVXAggregateStatisticsRequest extends OFAggregateStatisticsRequest
		implements DevirtualizableStatistic {

	Logger log = LogManager.getLogger(OVXAggregateStatisticsRequest.class.getName());
	
	@Override
	public void devirtualizeStatistic(final OVXSwitch sw,
			final OVXStatisticsRequest msg) {
		
		OVXAggregateStatisticsReply stat = new OVXAggregateStatisticsReply();
		int tid = sw.getTenantId();
		HashSet<Long> uniqueCookies = new HashSet<Long>();
		
		if ((this.match.getWildcardObj().isFull() || this.match.getWildcards() == -1) // the -1 is for beacon...
				&& this.outPort == OFPort.OFPP_NONE.getValue()) {
			FlowTable ft = sw.getFlowTable();
			stat.setFlowCount(ft.getFlowTable().size());
			stat.setByteCount(0);
			stat.setPacketCount(0);
			for (PhysicalSwitch psw : sw.getPhysicalSwitches()) {
				List<OVXFlowStatisticsReply> reps = psw.getFlowStats(tid);
				if (reps != null) {
					for (OVXFlowStatisticsReply s : reps) {
						
						if (!uniqueCookies.contains(s.getCookie())) {
							
							stat.setByteCount(stat.getByteCount() + s.getByteCount());
							stat.setByteCount(stat.getPacketCount() + s.getPacketCount());
							uniqueCookies.add(s.getCookie());
							
						}
					}
					
				}
			}
		}
		
		
		OVXStatisticsReply reply = new OVXStatisticsReply();
		reply.setXid(msg.getXid());
		reply.setStatisticType(OFStatisticsType.AGGREGATE);
		reply.setStatistics(Collections.singletonList(stat));
			
		reply.setLengthU(OVXStatisticsReply.MINIMUM_LENGTH + stat.getLength());
			
		sw.sendMsg(reply, sw);
		
	}
	
}
