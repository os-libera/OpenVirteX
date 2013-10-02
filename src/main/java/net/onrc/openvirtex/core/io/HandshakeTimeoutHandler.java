/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.core.io;

import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.exceptions.HandshakeTimeoutException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

/**
 * Timeout if the switch takes too long to connect.
 * 
 * Timeout values set in switch pipeline class.
 */
public class HandshakeTimeoutHandler extends SimpleChannelUpstreamHandler {
	static final HandshakeTimeoutException EXCEPTION = new HandshakeTimeoutException();

	final OFChannelHandler channelHandler;
	final Timer timer;
	final long timeoutNanos;
	volatile Timeout timeout;

	public HandshakeTimeoutHandler(final OFChannelHandler channelHandler,
			final Timer timer, final long timeoutSeconds) {
		super();
		this.channelHandler = channelHandler;
		this.timer = timer;
		this.timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);

	}

	@Override
	public void channelOpen(final ChannelHandlerContext ctx,
			final ChannelStateEvent e) throws Exception {
		if (this.timeoutNanos > 0) {
			this.timeout = this.timer.newTimeout(new HandshakeTimeoutTask(ctx),
					this.timeoutNanos, TimeUnit.NANOSECONDS);
		}
		ctx.sendUpstream(e);
	}

	@Override
	public void channelClosed(final ChannelHandlerContext ctx,
			final ChannelStateEvent e) throws Exception {
		if (this.timeout != null) {
			this.timeout.cancel();
			this.timeout = null;
		}
	}

	private final class HandshakeTimeoutTask implements TimerTask {

		private final ChannelHandlerContext ctx;

		HandshakeTimeoutTask(final ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void run(final Timeout timeout) throws Exception {
			if (timeout.isCancelled()) {
				return;
			}

			if (!this.ctx.getChannel().isOpen()) {
				return;
			}
			if (!HandshakeTimeoutHandler.this.channelHandler
					.isHandShakeComplete()) {
				Channels.fireExceptionCaught(this.ctx,
						HandshakeTimeoutHandler.EXCEPTION);
			}
		}
	}
}
