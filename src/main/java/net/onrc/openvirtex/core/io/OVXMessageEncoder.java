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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * encode an openflow message into a netty Channel.
 *
 * @author alshabib
 */
public class OVXMessageEncoder extends OneToOneEncoder {

    Logger log = LogManager.getLogger(OVXMessageEncoder.class.getName());

    @Override
    protected Object encode(final ChannelHandlerContext ctx,
                            final Channel channel, final Object msg) throws Exception {
        if (!(msg instanceof List)) {
            return msg;
        }

        @SuppressWarnings("unchecked")
        final List<OFMessage> msglist = (List<OFMessage>) msg;
        /*int size = 0;
        for (final OFMessage ofm : msglist) {
            size += ofm.getLengthU();
        }

        final ChannelBuffer buf = ChannelBuffers.buffer(size);

        for (final OFMessage ofm : msglist) {

            ofm.writeTo(buf);

        }*/
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        for (OFMessage ofm : msglist) {
            if (ofm != null) {
                //this.log.info(ofm.toString());

                ofm.writeTo(buf);
            }
        }

        return buf;
    }

}
