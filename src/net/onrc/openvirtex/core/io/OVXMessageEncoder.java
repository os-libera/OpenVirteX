/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.core.io;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.openflow.protocol.OFMessage;

/**
 * encode an openflow message into a netty Channel.
 * 
 * @author alshabib
 */
public class OVXMessageEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		if (!(msg instanceof List))
			return msg;

		@SuppressWarnings("unchecked")
		List<OFMessage> msglist = (List<OFMessage>) msg;
		int size = 0;
		for (OFMessage ofm : msglist) {
			size += ofm.getLengthU();
		}

		ChannelBuffer buf = ChannelBuffers.buffer(size);
		;
		for (OFMessage ofm : msglist) {
			ofm.writeTo(buf);
		}
		return buf;
	}

}
