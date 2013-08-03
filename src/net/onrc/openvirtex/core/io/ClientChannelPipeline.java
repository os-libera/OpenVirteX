package net.onrc.openvirtex.core.io;

import java.util.concurrent.ThreadPoolExecutor;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;

public class ClientChannelPipeline  extends OpenflowChannelPipeline{

    private ClientBootstrap bootstrap = null;
    private OVXSwitch sw = null;
    

    public ClientChannelPipeline(OpenVirteXController openVirteXController,
	    ThreadPoolExecutor pipelineExecutor, ClientBootstrap bootstrap, OVXSwitch sw) {
	super();
	this.ctrl = openVirteXController;
	this.pipelineExecutor = pipelineExecutor;
	this.timer = new HashedWheelTimer();
	this.idleHandler = new IdleStateHandler(timer, 20, 25, 0);
	this.readTimeoutHandler = new ReadTimeoutHandler(timer, 30);
	this.bootstrap  = bootstrap;
	this.sw  = sw;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
	ControllerChannelHandler handler = new ControllerChannelHandler(ctrl, sw);

	ChannelPipeline pipeline = Channels.pipeline();
	pipeline.addLast("reconnect", new ReconnectHandler(sw, bootstrap, timer, 15));
	pipeline.addLast("ofmessagedecoder", new OVXMessageDecoder());
	pipeline.addLast("ofmessageencoder", new OVXMessageEncoder());
	pipeline.addLast("idle", idleHandler);
	pipeline.addLast("timeout", readTimeoutHandler);
	pipeline.addLast("handshaketimeout", new HandshakeTimeoutHandler(
		handler, timer, 15));
	if (pipelineExecutor == null)
	    pipelineExecutor = new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576);
	pipeline.addLast("pipelineExecutor", new ExecutionHandler(
		pipelineExecutor));
	pipeline.addLast("handler", handler);
	return pipeline;
    }

}
