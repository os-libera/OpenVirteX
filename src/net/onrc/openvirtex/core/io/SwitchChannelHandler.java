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

/**
 * author: alshabib
 * 
 * heavily inspired from floodlight.
 * 
 */
package net.onrc.openvirtex.core.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.exceptions.HandshakeTimeoutException;
import net.onrc.openvirtex.exceptions.SwitchStateException;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFGetConfigRequest;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFQueueGetConfigReply;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFSwitchConfig;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;

public class SwitchChannelHandler extends OFChannelHandler {

    Logger log = LogManager.getLogger(SwitchChannelHandler.class.getName());
    protected ArrayList<OFPortStatus> pendingPortStatusMsg = null;

    /*
     * 
     * The enum below implements the connection state machine. Each method in
     * individual enum elements override previous implementations of each
     * message processor. Each state expects some event and passes to the next
     * state.
     */
    enum ChannelState {
	INIT(false) {

	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m)
		    throws IOException {
		// no need to actually implement
		// because it won't happen because nothing
		// is connected to us.

	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		unhandledMessageReceived(h, m);
	    }

	},
	WAIT_HELLO(false) {

	    @Override
	    void processOFHello(SwitchChannelHandler h, OFHello m)
		    throws IOException {
		h.sendHandShakeMessage(OFType.FEATURES_REQUEST);
		h.setState(WAIT_FEATURES_REPLY);
	    }

	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m) {
		h.log.error("Error waiting for Hello (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());

		h.channel.disconnect();
	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		unhandledMessageReceived(h, m);
	    }
	},
	WAIT_FEATURES_REPLY(false) {

	    @Override
	    void processOFFeaturesReply(SwitchChannelHandler h,
		    OFFeaturesReply m) throws IOException {
		h.featuresReply = m;
		h.sendHandshakeSetConfig();
		h.setState(WAIT_CONFIG_REPLY);
	    }

	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m) {
		h.log.error("Error waiting for features (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());
		h.channel.disconnect();
	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		unhandledMessageReceived(h, m);
	    }
	},
	WAIT_CONFIG_REPLY(false) {

	    @Override
	    void processOFGetConfigReply(SwitchChannelHandler h,
		    OFGetConfigReply m) throws IOException {
		if (m.getMissSendLength() != (short) 0xffff) {
		    h.log.error("Miss send length was not set properly by switch {}",
			    h.featuresReply.getDatapathId());
		}
		h.sendHandshakeDescriptionStatsRequest();
		h.setState(WAIT_DESCRIPTION_STAT_REPLY);
	    }

	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m)
		    throws IOException {
		h.log.error("Error waiting for config reply (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());
		h.channel.disconnect();
	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		h.pendingPortStatusMsg.add(m);

	    }
	},
	WAIT_DESCRIPTION_STAT_REPLY(false) {

	    @Override
	    void processOFStatisticsReply(SwitchChannelHandler h,
		    OFStatisticsReply m) {
		// Read description, if it has been updated
		OVXDescriptionStatistics description = new OVXDescriptionStatistics();
		ChannelBuffer data = ChannelBuffers.buffer(description
			.getLength());
		OFStatistics f = m.getFirstStatistics();
		f.writeTo(data);
		description.readFrom(data);
		h.sw = new PhysicalSwitch();
		// set switch information
		// set features reply and channel first so we a DPID and
		// channel info.
		h.sw.setFeaturesReply(h.featuresReply);
		h.sw.setDescriptionStats(description);
		h.sw.setConnected(true);
		h.sw.setChannel(h.channel);

		for (OFPortStatus ps : h.pendingPortStatusMsg)
		    handlePortStatusMessage(h, ps);
		h.pendingPortStatusMsg.clear();
		h.sw.init();
		h.setState(ACTIVE);
	    }

	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m)
		    throws IOException {
		h.log.error("Error waiting for desc stats reply (type:{}, code:{})", 
			m.getErrorType(), m.getErrorCode());
		h.channel.disconnect();

	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		h.pendingPortStatusMsg.add(m);

	    }
	},
	ACTIVE(true) {

	    @Override
	    void processOFMessage(SwitchChannelHandler h, OFMessage m)
		    throws IOException {

		switch (m.getType()) {
		    case ECHO_REQUEST:
			processOFEchoRequest(h, (OFEchoRequest) m);
			break;
		    case BARRIER_REPLY:
		    case ECHO_REPLY:
			// do nothing but thank the switch
			break;
		    case HELLO:
			h.sendHandShakeMessage(OFType.FEATURES_REQUEST);
			break;
		    case FEATURES_REPLY:
			h.featuresReply = (OFFeaturesReply) m;
			h.sw.setFeaturesReply(h.featuresReply);
			break;
		    case ERROR:
		    case FLOW_REMOVED:
		    case GET_CONFIG_REPLY:
		    case PACKET_IN:
		    case PORT_STATUS:
		    case QUEUE_GET_CONFIG_REPLY:
		    case STATS_REPLY:
		    case VENDOR:
			h.sw.handleIO(m);
			break;
			// The following messages are sent to switches. The controller
			// should never receive them
		    case SET_CONFIG:
		    case GET_CONFIG_REQUEST:
		    case PACKET_OUT:
		    case PORT_MOD:
		    case QUEUE_GET_CONFIG_REQUEST:
		    case BARRIER_REQUEST:
		    case STATS_REQUEST:
		    case FEATURES_REQUEST:
		    case FLOW_MOD:
			illegalMessageReceived(h, m);
			break;
		}

	    }


	    @Override
	    void processOFError(SwitchChannelHandler h, OFError m)
		    throws IOException {
		//should never happen

	    }

	    @Override
	    void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		    throws IOException {
		//should never happen

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
	protected String getSwitchStateMessage(SwitchChannelHandler h,
		OFMessage m, String details) {
	    return String.format("Switch: [%s], State: [%s], received: [%s]"
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
	protected void illegalMessageReceived(SwitchChannelHandler h,
		OFMessage m) {
	    String msg = getSwitchStateMessage(h, m,
		    "Switch should never send this message in the current state");
	    throw new SwitchStateException(msg);

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
	protected void unhandledMessageReceived(SwitchChannelHandler h,
		OFMessage m) {
	    h.log.warn(getSwitchStateMessage(h, m,
		    "Received unhandled message; moving swiftly along..."));
	}

	/**
	 * Handle a port status message.
	 * 
	 * Handle a port status message by updating the port maps in a
	 * switch instance and notifying Controller about the change so it
	 * can dispatch a switch update.
	 * 
	 * @param h
	 *            The OFChannelHhandler that received the message
	 * @param m
	 *            The PortStatus message we received
	 * @param doNotify
	 *            if true switch port changed events will be dispatched
	 */
	protected void handlePortStatusMessage(SwitchChannelHandler h,
		OFPortStatus m) {
	    if (h.sw == null) {
		String msg = getSwitchStateMessage(h, m,
			"State machine error: switch is null. Should never "
				+ "happen");
		throw new SwitchStateException(msg);
	    }
	    h.sw.handleIO(m);
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
	void processOFMessage(SwitchChannelHandler h, OFMessage m)
		throws IOException {
	    switch (m.getType()) {
		case HELLO:
		    processOFHello(h, (OFHello) m);
		    break;
		case BARRIER_REPLY:
		    processOFBarrierReply(h, (OFBarrierReply) m);
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
		case FEATURES_REPLY:
		    processOFFeaturesReply(h, (OFFeaturesReply) m);
		    break;
		case FLOW_REMOVED:
		    processOFFlowRemoved(h, (OFFlowRemoved) m);
		    break;
		case GET_CONFIG_REPLY:
		    processOFGetConfigReply(h, (OFGetConfigReply) m);
		    break;
		case PACKET_IN:
		    processOFPacketIn(h, (OFPacketIn) m);
		    break;
		case PORT_STATUS:
		    processOFPortStatus(h, (OFPortStatus) m);
		    break;
		case QUEUE_GET_CONFIG_REPLY:
		    processOFQueueGetConfigReply(h, (OFQueueGetConfigReply) m);
		    break;
		case STATS_REPLY:
		    processOFStatisticsReply(h, (OFStatisticsReply) m);
		    break;
		case VENDOR:
		    processOFVendor(h, (OFVendor) m);
		    break;
		    // The following messages are sent to switches. The controller
		    // should never receive them
		case SET_CONFIG:
		case GET_CONFIG_REQUEST:
		case PACKET_OUT:
		case PORT_MOD:
		case QUEUE_GET_CONFIG_REQUEST:
		case BARRIER_REQUEST:
		case STATS_REQUEST:
		case FEATURES_REQUEST:
		case FLOW_MOD:
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

	void processOFHello(SwitchChannelHandler h, OFHello m)
		throws IOException {
	    // we only expect hello in the WAIT_HELLO state
	    illegalMessageReceived(h, m);
	}

	void processOFBarrierReply(SwitchChannelHandler h, OFBarrierReply m)
		throws IOException {
	    // Silently ignore.
	}

	void processOFEchoRequest(SwitchChannelHandler h, OFEchoRequest m)
		throws IOException {
	    OFEchoReply reply = (OFEchoReply) BasicFactory.getInstance()
		    .getMessage(OFType.ECHO_REPLY);
	    reply.setXid(m.getXid());
	    reply.setPayload(m.getPayload());
	    reply.setLengthU(m.getLengthU());
	    h.channel.write(Collections.singletonList(reply));
	}

	void processOFEchoReply(SwitchChannelHandler h, OFEchoReply m)
		throws IOException {
	    // Do nothing with EchoReplies !!
	}

	// no default implementation for OFError
	// every state must override it
	abstract void processOFError(SwitchChannelHandler h, OFError m)
		throws IOException;

	void processOFFeaturesReply(SwitchChannelHandler h, OFFeaturesReply m)
		throws IOException {
	    unhandledMessageReceived(h, m);
	}

	void processOFFlowRemoved(SwitchChannelHandler h, OFFlowRemoved m)
		throws IOException {
	    unhandledMessageReceived(h, m);
	}

	void processOFGetConfigReply(SwitchChannelHandler h, OFGetConfigReply m)
		throws IOException {
	    
	    illegalMessageReceived(h, m);
	}

	void processOFPacketIn(SwitchChannelHandler h, OFPacketIn m)
		throws IOException {
	    unhandledMessageReceived(h, m);
	}

	// bi default implementation. Every state needs to handle it.
	abstract void processOFPortStatus(SwitchChannelHandler h, OFPortStatus m)
		throws IOException;

	void processOFQueueGetConfigReply(SwitchChannelHandler h,
		OFQueueGetConfigReply m) throws IOException {
	    unhandledMessageReceived(h, m);
	}

	void processOFStatisticsReply(SwitchChannelHandler h,
		OFStatisticsReply m) throws IOException {
	    unhandledMessageReceived(h, m);
	}

	void processOFVendor(SwitchChannelHandler h, OFVendor m)
		throws IOException {
	    
	    unhandledMessageReceived(h, m);
	}
    }

    private ChannelState state;
    private OFFeaturesReply featuresReply;

    /*
     * Transaction ids to use during initialization
     */
    private int handshakeTransactionIds = -1;

    public SwitchChannelHandler(OpenVirteXController ctrl) {
	this.ctrl = ctrl;
	this.state = ChannelState.INIT;
	this.pendingPortStatusMsg = new ArrayList<OFPortStatus>();
    }

    @Override
    public boolean isHandShakeComplete() {
	return this.state.isHandShakeComplete();
    }

    /**
     * Return a string describing this switch based on the already available
     * information (DPID and/or remote socket)
     * 
     * @return
     */
    @Override
    protected String getSwitchInfoString() {
	if (sw != null)
	    return sw.toString();
	String channelString;
	if (channel == null || channel.getRemoteAddress() == null) {
	    channelString = "?";
	} else {
	    channelString = channel.getRemoteAddress().toString();
	}
	String dpidString;
	if (featuresReply == null) {
	    dpidString = "?";
	} else {
	    dpidString = HexString.toHexString(featuresReply.getDatapathId());
	}
	return String.format("DPID -> %s(%s)", dpidString, channelString);
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

    /**
     * Send a message to the switch using the handshake transactions ids.
     * 
     * @throws IOException
     */

    @Override
    protected void sendHandShakeMessage(OFType type) throws IOException {
	OFMessage m = BasicFactory.getInstance().getMessage(type);
	m.setXid(handshakeTransactionIds--);
	channel.write(Collections.singletonList(m));
    }

    /**
     * Send the configuration requests to tell the switch we want full packets
     * 
     * @throws IOException
     */
    private void sendHandshakeSetConfig() throws IOException {
	List<OFMessage> msglist = new ArrayList<OFMessage>(3);

	// Ensure we receive the full packet via PacketIn
	
	OFSetConfig configSet = (OFSetConfig) BasicFactory.getInstance()
		.getMessage(OFType.SET_CONFIG);
	configSet.setMissSendLength((short) 0xffff).setLengthU(
		OFSwitchConfig.MINIMUM_LENGTH);
	configSet.setXid(handshakeTransactionIds--);
	msglist.add(configSet);

	// Barrier
	OFBarrierRequest barrier = (OFBarrierRequest) BasicFactory
		.getInstance().getMessage(OFType.BARRIER_REQUEST);
	barrier.setXid(handshakeTransactionIds--);
	msglist.add(barrier);

	// Verify (need barrier?)
	OFGetConfigRequest configReq = (OFGetConfigRequest) BasicFactory
		.getInstance().getMessage(OFType.GET_CONFIG_REQUEST);
	configReq.setXid(handshakeTransactionIds--);
	msglist.add(configReq);
	channel.write(msglist);
    }

    protected void sendHandshakeDescriptionStatsRequest() {
	OFStatisticsRequest req = new OFStatisticsRequest();
	req.setStatisticType(OFStatisticsType.DESC);
	req.setXid(handshakeTransactionIds--);

	channel.write(Collections.singletonList(req));

    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
	    throws Exception {
	OFMessage m = BasicFactory.getInstance()
		.getMessage(OFType.ECHO_REQUEST);
	e.getChannel().write(Collections.singletonList(m));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	    throws Exception {

	/*
	 * Pass all messages to the handlers, except LLDP which we send staight to
	 * the topology controller.
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
			case PACKET_IN:
			    /*
			     * Is this packet a packet in? If yes is it an lldp?
			     * then send it to the PhysicalTopoHandler.
			     */
			    byte[] data = ((OFPacketIn) ofm).getPacketData();
			    if (data.length > 14) {
				if ((data[12] == (byte) 0x88)
					&& (data[13] == (byte) 0xcc)) {
				    log.warn("Got LLDP; send to physicalnetwork. unimplemented"); 
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

    /*
     * Set the state for this channel
     */
    private void setState(ChannelState state) {
	this.state = state;
    }

}
