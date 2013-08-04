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
    private Integer maxBackOff;

    private OVXSwitch sw;

    private ChannelGroup cg;



    public ReconnectHandler(OVXSwitch sw, ClientBootstrap bootstrap, 
	    Timer timer, int maxBackOff, ChannelGroup cg) {
	super();
	this.sw = sw;
	this.bootstrap = bootstrap;
	this.timer = timer;
	this.maxBackOff = maxBackOff;
	this.cg = cg;


    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
	int retry = sw.incrementBackOff();
	Integer backOffTime = Math.min(1<<retry, maxBackOff);
	timeout = timer.newTimeout(new ReconnectTimeoutTask(this.sw, this.cg),
		backOffTime, TimeUnit.SECONDS);
	log.error("Backing off {} for controller {}", backOffTime, bootstrap.getOption("remoteAddress"));
	ctx.sendUpstream(e);

    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
	this.sw.resetBackOff();
	ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {


	Throwable cause = e.getCause();
	if (cause instanceof ConnectException) {
	    return;
	}

	ctx.sendUpstream(e);
    }

    private final class ReconnectTimeoutTask implements TimerTask {

	OVXSwitch sw = null;
	private ChannelGroup cg;

	public ReconnectTimeoutTask(OVXSwitch sw, ChannelGroup cg) {
	    this.sw = sw;
	    this.cg = cg;
	}

	@Override
	public void run(Timeout timeout) throws Exception {

	    final InetSocketAddress remoteAddr = (InetSocketAddress) bootstrap.getOption("remoteAddress");
	    ChannelFuture cf = bootstrap.connect();

	    cf.addListener(new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture e) throws Exception {
		    if (e.isSuccess()) {
			sw.setChannel(e.getChannel());
			cg.add(e.getChannel());
		    } else
			log.error("Failed to connect to controller {} for switch {}", remoteAddr, sw.getSwitchId());

		}
	    });

	}
    }



}
