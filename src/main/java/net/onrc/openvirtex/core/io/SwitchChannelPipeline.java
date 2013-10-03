/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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

import java.util.concurrent.ThreadPoolExecutor;

import net.onrc.openvirtex.core.OpenVirteXController;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;

public class SwitchChannelPipeline extends OpenflowChannelPipeline {

	public SwitchChannelPipeline(
			final OpenVirteXController openVirteXController,
			final ThreadPoolExecutor pipelineExecutor) {
		super();
		this.ctrl = openVirteXController;
		this.pipelineExecutor = pipelineExecutor;
		this.timer = new HashedWheelTimer();
		this.idleHandler = new IdleStateHandler(this.timer, 20, 25, 0);
		this.readTimeoutHandler = new ReadTimeoutHandler(this.timer, 30);
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		final SwitchChannelHandler handler = new SwitchChannelHandler(this.ctrl);

		final ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("ofmessagedecoder", new OVXMessageDecoder());
		pipeline.addLast("ofmessageencoder", new OVXMessageEncoder());
		pipeline.addLast("idle", this.idleHandler);
		pipeline.addLast("timeout", this.readTimeoutHandler);
		pipeline.addLast("handshaketimeout", new HandshakeTimeoutHandler(
				handler, this.timer, 15));
		if (this.pipelineExecutor == null) {
			this.pipelineExecutor = new OrderedMemoryAwareThreadPoolExecutor(
					16, 1048576, 1048576);
		}
		pipeline.addLast("pipelineExecutor", new ExecutionHandler(
				this.pipelineExecutor));
		pipeline.addLast("handler", handler);
		return pipeline;
	}

}
