/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.Collections;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;

public class DummyPhysicalSwitch extends PhysicalSwitch {

    public DummyPhysicalSwitch(final long dpid) {
        super(dpid);
        this.setDescriptionStats(new OVXDescriptionStatistics());
        final OFFeaturesReply offr = new OFFeaturesReply();
        offr.setPorts(Collections.singletonList(new OFPhysicalPort()));
        this.featuresReply = offr;
        final Channel ch = new TestChannelClass();
        this.channel = ch;
        this.setConnected(true);
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        if (this.channel.isOpen() && this.isConnected) {
            this.channel.write(Collections.singletonList(msg));
        }
    }

}
