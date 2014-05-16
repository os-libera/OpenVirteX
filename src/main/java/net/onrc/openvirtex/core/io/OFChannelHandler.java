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
package net.onrc.openvirtex.core.io;

import java.io.IOException;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.datapath.Switch;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.openflow.protocol.OFType;

public abstract class OFChannelHandler extends IdleStateAwareChannelHandler {

    @SuppressWarnings("rawtypes")
    protected Switch sw;
    protected Channel channel;
    protected OpenVirteXController ctrl;

    public abstract boolean isHandShakeComplete();

    protected abstract String getSwitchInfoString();

    protected abstract void sendHandShakeMessage(OFType type)
            throws IOException;

}
