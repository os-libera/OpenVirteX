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
package net.onrc.openvirtex.api.service.handlers.tenant;

import org.openflow.protocol.OFMessage;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

public class TestSwitch extends PhysicalSwitch {

    public TestSwitch(long dpid) {
        super(dpid);
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        /*
         * Hack to avoid NPE from not setting a channel for tests. Either this
         * class or a dummy Channel implementation...
         */
    }

}
