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

import java.util.List;

import net.onrc.openvirtex.messages.OVXMessageFactory;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.openflow.protocol.OFMessage;

/**
 * Decode an openflow message from a netty Channel.
 *
 * @author alshabib
 */
public class OVXMessageDecoder extends FrameDecoder {

    OVXMessageFactory factory = OVXMessageFactory.getInstance();

    @Override
    protected Object decode(final ChannelHandlerContext ctx,
            final Channel channel, final ChannelBuffer buffer) throws Exception {
        if (!channel.isConnected()) {
            // if the channel is closed, there will be nothing to read.
            return null;
        }

        final List<OFMessage> message = this.factory.parseMessage(buffer);
        return message;
    }

}
