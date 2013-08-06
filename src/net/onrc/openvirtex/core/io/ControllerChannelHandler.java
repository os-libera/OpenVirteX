package net.onrc.openvirtex.core.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ControllerStateException;
import net.onrc.openvirtex.exceptions.HandshakeTimeoutException;
import net.onrc.openvirtex.exceptions.SwitchStateException;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.openflow.protocol.OFBarrierReply;
import org.openflow.protocol.OFBarrierRequest;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFEchoRequest;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigRequest;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPortMod;
import org.openflow.protocol.OFQueueGetConfigRequest;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;



public class ControllerChannelHandler extends OFChannelHandler {

    private Logger log = LogManager.getLogger(ControllerChannelHandler.class.getName());

    enum ChannelState {
	INIT(false) {

	    @Override
	    void processOFError(ControllerChannelHandler h, OFError m)
		    throws IOException {
		//This should never happen. We haven't connected to anyone
		// yet.

	    }



	}, 
	WAIT_HELLO(false) {

	    @Override
	    void processOFError(ControllerChannelHandler h, OFError m)
		    throws IOException {
		h.log.error("Error waiting for Hello (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());
		
		h.channel.disconnect();   
	    }

	    @Override
	    void processOFHello(ControllerChannelHandler h, OFHello m) 
		    throws IOException {
		if (m.getVersion() == OFMessage.OFP_VERSION) {
		    h.setState(WAIT_FT_REQ);
		} else {
		    h.log.error("Unsupported OpenFlow Version");
		    OFError error = new OFError();
		    error.setErrorType(OFErrorType.OFPET_HELLO_FAILED);
		    error.setErrorCode(OFError.OFHelloFailedCode.OFPHFC_INCOMPATIBLE);
		    error.setVersion(OFMessage.OFP_VERSION);
		    String errmsg = "we only support version "
			    + Integer.toHexString(OFMessage.OFP_VERSION)
			    + " and you are not it";
		    error.setError(errmsg.getBytes());
		    error.setErrorIsAscii(true);
		    h.channel.disconnect();
		}
	    }
	},
	WAIT_FT_REQ(false) {

	    @Override
	    void processOFError(ControllerChannelHandler h, OFError m)
		    throws IOException {
		h.log.error("Error waiting for Features Request (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());
		
		h.channel.disconnect(); 

	    }

	    @Override
	    void processOFFeaturesRequest(ControllerChannelHandler h, OFFeaturesRequest m) {
		OFFeaturesReply reply = h.sw.getFeaturesReply();
		if (reply == null) {
		    h.log.error("OVXSwitch failed to return a featuresReply message: {}" + h.sw.getSwitchId());
		    h.channel.disconnect();
		}
		reply.setXid(m.getXid());
		h.channel.write(Collections.singletonList(reply));
		h.log.info("Connected dpid {} to controller {}", reply.getDatapathId(), h.channel.getRemoteAddress());
		h.setState(ACTIVE);
	    }

	},
	ACTIVE(true) {

	    @Override
	    void processOFError(ControllerChannelHandler h, OFError m)
		    throws IOException {
		h.sw.handleIO(m);
	    }

	    @Override
	    void processOFMessage(ControllerChannelHandler h, OFMessage m)
		    throws IOException {

		switch (m.getType()) {
		    case HELLO:
			processOFHello(h, (OFHello) m);
			break;    
		    case ECHO_REPLY:
			break;
		    case ECHO_REQUEST:
			processOFEchoRequest(h, (OFEchoRequest) m);
			break;


		    case FEATURES_REQUEST:
			processOFFeaturesRequest(h, (OFFeaturesRequest) m);
			break;
		    case BARRIER_REQUEST:
			//TODO: actually implement barrier contract
			OFBarrierReply breply = new OFBarrierReply();
			breply.setXid(m.getXid());
			h.channel.write(Collections.singletonList(breply));
			break;
		    case SET_CONFIG:
		    case ERROR:
		    case PACKET_OUT:
		    case PORT_MOD:
		    case QUEUE_GET_CONFIG_REQUEST:
		    case STATS_REQUEST:    
		    case FLOW_MOD:
		    case GET_CONFIG_REQUEST:
			h.sw.handleIO(m);
			break;
		    case VENDOR:
			unhandledMessageReceived(h, m);
			break;
		    case FEATURES_REPLY:
		    case FLOW_REMOVED:
		    case PACKET_IN:
		    case PORT_STATUS:
		    case BARRIER_REPLY:
		    case GET_CONFIG_REPLY:
		    case STATS_REPLY:
		    case QUEUE_GET_CONFIG_REPLY:
			illegalMessageReceived(h, m);
			break;
		}
	    }



	};

	private boolean handshakeComplete = false;

	ChannelState(boolean handshakeComplete) {
	    this.handshakeComplete = handshakeComplete;
	}

	public boolean isHandShakeComplete() {
	    return this.handshakeComplete;
	}

	/**
	 * Get a string specifying the switch connection, state, and message
	 * received. To be used as message for SwitchStateException or log
	 * messages
	 * 
	 * @param h
	 *            The channel handler (to get switch information_
	 * @param m
	 *            The OFMessage that has just been received
	 * @param details
	 *            A string giving more details about the exact nature of the
	 *            problem.
	 * @return
	 */
	// needs to be protected because enum members are acutally subclasses
	protected String getControllerStateMessage(ControllerChannelHandler h,
		OFMessage m, String details) {
	    return String.format("Controller: [%s], State: [%s], received: [%s]"
		    + ", details: %s", h.getSwitchInfoString(),
		    this.toString(), m.getType().toString(), details);
	}

	/**
	 * We have an OFMessage we didn't expect given the current state and we
	 * want to treat this as an error. We currently throw an exception that
	 * will terminate the connection However, we could be more forgiving
	 * 
	 * @param h
	 *            the channel handler that received the message
	 * @param m
	 *            the message
	 * @throws SwitchStateExeption
	 *             we always through the execption
	 */
	// needs to be protected because enum members are acutally subclasses
	protected void illegalMessageReceived(ControllerChannelHandler h,
		OFMessage m) {
	    String msg = getControllerStateMessage(h, m,
		    "Controller should never send this message in the current state");
	    throw new ControllerStateException(msg);

	}

	/**
	 * We have an OFMessage we didn't expect given the current state and we
	 * want to ignore the message
	 * 
	 * @param h
	 *            the channel handler the received the message
	 * @param m
	 *            the message
	 */
	protected void unhandledMessageReceived(ControllerChannelHandler h,
		OFMessage m) {
	   h.log.warn("Received currently unhandled message {}", m);
	}


	/**
	 * Process an OF message received on the channel and update state
	 * accordingly.
	 * 
	 * The main "event" of the state machine. Process the received message,
	 * send follow up message if required and update state if required.
	 * 
	 * Switches on the message type and calls more specific event handlers
	 * for each individual OF message type. If we receive a message that is
	 * supposed to be sent from a controller to a switch we throw a
	 * SwitchStateExeption.
	 * 
	 * The more specific handlers can also throw SwitchStateExceptions
	 * 
	 * @param h
	 *            The SwitchChannelHandler that received the message
	 * @param m
	 *            The message we received.
	 * @throws SwitchStateException
	 * @throws IOException
	 */
	void processOFMessage(ControllerChannelHandler h, OFMessage m)
		throws IOException {
	    switch (m.getType()) {
		case HELLO:
		    processOFHello(h, (OFHello) m);
		    break;    
		case ECHO_REPLY:
		    processOFEchoReply(h, (OFEchoReply) m);
		    break;
		case ECHO_REQUEST:
		    processOFEchoRequest(h, (OFEchoRequest) m);
		    break;
		case ERROR:
		    processOFError(h, (OFError) m);
		    break;
		case VENDOR:
		    processOFVendor(h, (OFVendor) m);
		    break;
		    // The following messages are sent to switches. The controller
		    // should never receive them
		case SET_CONFIG:
		    processOFSetConfig(h, (OFSetConfig) m);
		    break;
		case PACKET_OUT:
		    processOFPacketOut(h, (OFPacketOut) m);
		    break;
		case PORT_MOD:
		    processOFPortMod(h, (OFPortMod) m);
		    break;
		case QUEUE_GET_CONFIG_REQUEST:
		    processOFQueueGetConfigRequest(h, (OFQueueGetConfigRequest)m);
		    break;
		case BARRIER_REQUEST:
		    processOFBarrierRequest(h, (OFBarrierRequest) m);
		    break;
		case STATS_REQUEST:
		    processOFStatsRequest(h, (OFStatisticsRequest) m);
		    break;
		case FEATURES_REQUEST:
		    processOFFeaturesRequest(h, (OFFeaturesRequest) m);
		    break;
		case FLOW_MOD:
		    processOFFlowMod(h, (OFFlowMod) m);
		    break;
		case GET_CONFIG_REQUEST:
		    processOFGetConfigRequest(h, (OFGetConfigRequest) m);
		    break;

		case FEATURES_REPLY:
		case FLOW_REMOVED:
		case PACKET_IN:
		case PORT_STATUS:
		case BARRIER_REPLY:
		case GET_CONFIG_REPLY:
		case STATS_REPLY:
		case QUEUE_GET_CONFIG_REPLY:
		    illegalMessageReceived(h, m);
		    break;
	    }
	}

	/*-----------------------------------------------------------------
	 * Default implementation for message handlers in any state.
	 *
	 * Individual states must override these if they want a behavior
	 * that differs from the default.
	 *
	 * In general, these handlers simply ignore the message and do
	 * nothing.
	 *
	 * There are some exceptions though, since some messages really
	 * are handled the same way in every state (e.g., ECHO_REQUST) or
	 * that are only valid in a single state (e.g., HELLO, GET_CONFIG_REPLY
	     -----------------------------------------------------------------*/

	void processOFHello(ControllerChannelHandler h, OFHello m)
		throws IOException {
	    // we only expect hello in the WAIT_HELLO state
	    illegalMessageReceived(h, m);
	}

	void processOFSetConfig(ControllerChannelHandler h, OFSetConfig m) {
	    illegalMessageReceived(h, m);
	}

	void processOFEchoRequest(ControllerChannelHandler h, OFEchoRequest m)
		throws IOException {
	    OFEchoReply reply = (OFEchoReply) BasicFactory.getInstance()
		    .getMessage(OFType.ECHO_REPLY);
	    reply.setXid(m.getXid());
	    reply.setPayload(m.getPayload());
	    reply.setLengthU(m.getLengthU());
	    h.channel.write(Collections.singletonList(reply));
	}

	void processOFEchoReply(ControllerChannelHandler h, OFEchoReply m)
		throws IOException {
	    // Do nothing with EchoReplies !!
	}

	// no default implementation for OFError
	// every state must override it
	abstract void processOFError(ControllerChannelHandler h, OFError m)
		throws IOException;

	void processOFPacketOut(ControllerChannelHandler h, OFPacketOut m) {
	    illegalMessageReceived(h, m);
	}


	void processOFPortMod(ControllerChannelHandler h, OFPortMod m) {
	    illegalMessageReceived(h, m);
	}

	void processOFQueueGetConfigRequest(ControllerChannelHandler h, OFQueueGetConfigRequest m) {
	    illegalMessageReceived(h, m);
	}

	void processOFBarrierRequest(ControllerChannelHandler h, OFBarrierRequest m) {
	    illegalMessageReceived(h, m);
	}

	void processOFStatsRequest(ControllerChannelHandler h, OFStatisticsRequest m) {
	    illegalMessageReceived(h, m);
	}

	void processOFFeaturesRequest(ControllerChannelHandler h, OFFeaturesRequest m) {
	    illegalMessageReceived(h, m);
	}

	void processOFFlowMod(ControllerChannelHandler h, OFFlowMod m) {
	    illegalMessageReceived(h, m);
	}

	void processOFGetConfigRequest(ControllerChannelHandler h, OFGetConfigRequest m) {
	    illegalMessageReceived(h, m);
	}

	void processOFVendor(ControllerChannelHandler h, OFVendor m)
		throws IOException {
	    
	    unhandledMessageReceived(h, m);
	}

    }




    private ChannelState state;
    private Integer handshakeTransactionIds = -1;



    public ControllerChannelHandler(OpenVirteXController ctrl, OVXSwitch sw) {
	this.ctrl = ctrl;
	this.state = ChannelState.INIT;
	this.sw = sw;
    }



    @Override
    public boolean isHandShakeComplete() {
	return this.state.isHandShakeComplete();
    }

    @Override
    protected String getSwitchInfoString() {
	return this.sw.toString();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	    throws Exception {
	channel = e.getChannel();
	sendHandShakeMessage(OFType.HELLO);
	setState(ChannelState.WAIT_HELLO);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
	    ChannelStateEvent e) throws Exception {

	if (this.sw != null) {
	   
	    this.sw.setConnected(false);
	    this.sw.tearDown();
	}

    }

    @Override
    protected void sendHandShakeMessage(OFType type) throws IOException {
	OFMessage m = BasicFactory.getInstance().getMessage(type);
	m.setXid(handshakeTransactionIds--);
	channel.write(Collections.singletonList(m));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	    throws Exception {

	/*
	 * Pass all messages to the handlers, except LLDP which goes to 
	 * the virtual network handler.
	 * 
	 * This should be implemented with a token bucket in order to rate limit
	 * the connections a little.
	 */
	if (e.getMessage() instanceof List) {
	    @SuppressWarnings("unchecked")
	    List<OFMessage> msglist = (List<OFMessage>) e.getMessage();

	    for (OFMessage ofm : msglist) {

		try {

		    switch (ofm.getType()) {
			case PACKET_OUT:
			    /*
			     * Is this packet a packet in? If yes is it an lldp?
			     * then send it to the PhysicalTopoHandler.
			     */
			    byte[] data = ((OFPacketOut) ofm).getPacketData();
			    if (data.length > 14) {
				if ((data[12] == (byte) 0x88)
					&& (data[13] == (byte) 0xcc)) {
				    log.warn("GOT LLDP; send it to virtual network. unimplemented.");
				    break;
				}
			    }
			default:
			    // Process all non-packet-ins
			    state.processOFMessage(this, ofm);
			    break;
		    }

		} catch (Exception ex) {
		    // We are the last handler in the stream, so run the
		    // exception through the channel again by passing in
		    // ctx.getChannel().
		    Channels.fireExceptionCaught(ctx.getChannel(), ex);
		}
	    }

	} else {
	    Channels.fireExceptionCaught(this.channel, new AssertionError(
		    "Message received from Channel is not a list"));
	}
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
	    throws Exception {
	OFMessage m = BasicFactory.getInstance()
		.getMessage(OFType.ECHO_REQUEST);
	e.getChannel().write(Collections.singletonList(m));
    }

    /*
     * Set the state for this channel
     */
    private void setState(ChannelState state) {
	this.state = state;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	    throws Exception {
	if (e.getCause() instanceof ReadTimeoutException) {
	    // switch timeout
	    log.error("Disconnecting switch {} due to read timeout ",
		    getSwitchInfoString(), e.getCause());
	    ctx.getChannel().close();
	} else if (e.getCause() instanceof HandshakeTimeoutException) {
	    log.error("Disconnecting switch {} failed to complete handshake ",
		    getSwitchInfoString(), e.getCause());
	    ctx.getChannel().close();
	} else if (e.getCause() instanceof ClosedChannelException) {
	    log.error("Channel for sw {} already closed",
		    getSwitchInfoString(), e.getCause());
	} else if (e.getCause() instanceof IOException) {
	    log.error("Disconnecting switch {} due to IO Error.",
		    getSwitchInfoString(), e.getCause());
	    ctx.getChannel().close();
	} else if (e.getCause() instanceof SwitchStateException) {
	    log.error("Disconnecting switch {} due to switch state error",
		    getSwitchInfoString(), e.getCause());
	    ctx.getChannel().close();
	} else if (e.getCause() instanceof MessageParseException) {
	    log.error("Disconnecting switch {} due to message parse failure",
		    getSwitchInfoString(), e.getCause());
	    ctx.getChannel().close();
	} else if (e.getCause() instanceof RejectedExecutionException) {
	    log.error("Could not process message: queue full", e.getCause());

	} else {

	    log.error("Error while processing message from switch {} state {}",
		    getSwitchInfoString(), this.state, e.getCause());

	    ctx.getChannel().close();
	    throw new RuntimeException(e.getCause());
	}
    }


}
