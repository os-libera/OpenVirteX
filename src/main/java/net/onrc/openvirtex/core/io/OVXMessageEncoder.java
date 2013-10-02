/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


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
	protected Object encode(final ChannelHandlerContext ctx,
			final Channel channel, final Object msg) throws Exception {
		if (!(msg instanceof List)) {
			return msg;
		}

		@SuppressWarnings("unchecked")
		final List<OFMessage> msglist = (List<OFMessage>) msg;
		int size = 0;
		for (final OFMessage ofm : msglist) {
			size += ofm.getLengthU();
		}

		final ChannelBuffer buf = ChannelBuffers.buffer(size);

		for (final OFMessage ofm : msglist) {

			ofm.writeTo(buf);

		}
		return buf;
	}

}
