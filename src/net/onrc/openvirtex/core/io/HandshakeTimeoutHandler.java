/**
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

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

    final OFChannelHandler                 channelHandler;
    final Timer                            timer;
    final long                             timeoutNanos;
    volatile Timeout                       timeout;

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
