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

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ReconnectException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

public class ReconnectHandler extends SimpleChannelHandler {

    Logger log = LogManager.getLogger(ReconnectHandler.class.getName());

    static final ReconnectException EXCEPTION = new ReconnectException();

    final ClientBootstrap bootstrap;
    final Timer timer;
    volatile Timeout timeout;
    private final Integer maxBackOff;

    private final OVXSwitch sw;

    private final ChannelGroup cg;

    public ReconnectHandler(final OVXSwitch sw,
            final ClientBootstrap bootstrap, final Timer timer,
            final int maxBackOff, final ChannelGroup cg) {
        super();
        this.sw = sw;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.maxBackOff = maxBackOff;
        this.cg = cg;

    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        if (!this.sw.isActive()) {
            return;
        }
        this.sw.removeChannel(e.getChannel());
        final int retry = this.sw.incrementBackOff();
        final Integer backOffTime = Math.min(1 << retry, this.maxBackOff);

        this.timeout = this.timer.newTimeout(new ReconnectTimeoutTask(this.sw,
                this.cg), backOffTime, TimeUnit.SECONDS);

        this.log.error("Backing off {} for controller {}", backOffTime,
                this.bootstrap.getOption("remoteAddress"));
        ctx.sendUpstream(e);

    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        ctx.sendUpstream(e);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        this.sw.resetBackOff();
        ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) {

        final Throwable cause = e.getCause();
        if (cause instanceof ConnectException) {
            return;
        }

        ctx.sendUpstream(e);
    }

    private final class ReconnectTimeoutTask implements TimerTask {

        OVXSwitch sw = null;
        private final ChannelGroup cg;

        public ReconnectTimeoutTask(final OVXSwitch sw, final ChannelGroup cg) {
            this.sw = sw;
            this.cg = cg;
        }

        @Override
        public void run(final Timeout timeout) throws Exception {

            final InetSocketAddress remoteAddr = (InetSocketAddress) ReconnectHandler.this.bootstrap
                    .getOption("remoteAddress");
            final ChannelFuture cf = ReconnectHandler.this.bootstrap.connect();

            cf.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture e)
                        throws Exception {
                    if (e.isSuccess()) {
                        ReconnectTimeoutTask.this.sw.setChannel(e.getChannel());
                        ReconnectTimeoutTask.this.cg.add(e.getChannel());
                    } else {
                        ReconnectHandler.this.log
                                .error("Failed to connect to controller {} for switch {}",
                                        remoteAddr,
                                        ReconnectTimeoutTask.this.sw
                                                .getSwitchName());
                    }

                }
            });

        }
    }

}
