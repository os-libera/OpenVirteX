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
package net.onrc.openvirtex.core.io;

import java.util.ArrayList;
import java.util.List;

//import net.onrc.openvirtex.messages.OVXMessageFactory;

import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.projectfloodlight.openflow.protocol.*;

/**
 * Decode an openflow message from a netty Channel.
 *
 * @author alshabib
 */
public class OVXMessageDecoder extends FrameDecoder {

    public static int MINIMUM_LENGTH = 8;
    Logger log = LogManager.getLogger(OVXMessageDecoder.class.getName());

    @Override
    protected Object decode(final ChannelHandlerContext ctx,
                            final Channel channel, final ChannelBuffer buffer) throws Exception {
        if (!channel.isConnected()) {
            // if the channel is closed, there will be nothing to read.
            return null;
        }

        //final List<OFMessage> message = this.factory.parseMessage(buffer);

        OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();

        final List<OVXMessage> msglist = new ArrayList<OVXMessage>();
        OFMessage msg = null;

        while (buffer.readableBytes() >= MINIMUM_LENGTH) {
            buffer.markReaderIndex();
            msg = reader.readFrom(buffer);

            if (msg == null) {
                buffer.resetReaderIndex();
                break;
            } else {
                //this.log.info(msg.toString());
                OVXMessage ovxmsg = OVXMessageUtil.toOVXMessage(msg);
                msglist.add(ovxmsg);
            }
        }

        /////////////////////////////////////////////////////////////////////////////////test
        if (msglist.size() == 0) {
            return null;
        }

        //return null;
        return msglist;
    }
}
