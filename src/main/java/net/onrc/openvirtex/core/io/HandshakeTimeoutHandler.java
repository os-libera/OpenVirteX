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
