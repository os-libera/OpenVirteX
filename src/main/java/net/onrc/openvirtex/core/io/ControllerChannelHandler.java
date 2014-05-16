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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ControllerStateException;
import net.onrc.openvirtex.exceptions.HandshakeTimeoutException;
import net.onrc.openvirtex.exceptions.SwitchStateException;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.packet.OVXLLDP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
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
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigRequest;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPortMod;
import org.openflow.protocol.OFQueueGetConfigRequest;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;
import org.openflow.vendor.nicira.OFNiciraVendorData;
import org.openflow.vendor.nicira.OFRoleRequestVendorData;

public class ControllerChannelHandler extends OFChannelHandler {

    private final Logger log = LogManager
            .getLogger(ControllerChannelHandler.class.getName());

    enum ChannelState {
        INIT(false) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                    final OFError m) throws IOException {
                // This should never happen. We haven't connected to anyone
                // yet.

            }

        },
        WAIT_HELLO(false) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                    final OFError m) throws IOException {
                h.log.error("Error waiting for Hello (type:{}, code:{})",
                        m.getErrorType(), m.getErrorCode());

                h.channel.disconnect();
            }

            @Override
            void processOFHello(final ControllerChannelHandler h,
                    final OFHello m) throws IOException {
                if (m.getVersion() == OFMessage.OFP_VERSION) {
                    h.setState(WAIT_FT_REQ);
                } else {
                    h.log.error("Unsupported OpenFlow Version");
                    final OFError error = new OFError();
                    error.setErrorType(OFErrorType.OFPET_HELLO_FAILED);
                    error.setErrorCode(OFError.OFHelloFailedCode.OFPHFC_INCOMPATIBLE);
                    error.setVersion(OFMessage.OFP_VERSION);
                    final String errmsg = "we only support version "
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
            void processOFError(final ControllerChannelHandler h,
                    final OFError m) throws IOException {
                h.log.error(
                        "Error waiting for Features Request (type:{}, code:{})",
                        m.getErrorType(), m.getErrorCode());

                h.channel.disconnect();

            }

            @Override
            void processOFFeaturesRequest(final ControllerChannelHandler h,
                    final OFFeaturesRequest m) {
                final OFFeaturesReply reply = h.sw.getFeaturesReply();
                if (reply == null) {
                    h.log.error("OVXSwitch failed to return a featuresReply message: {}"
                            + h.sw.getSwitchName());
                    h.channel.disconnect();
                }
                reply.setXid(m.getXid());
                h.channel.write(Collections.singletonList(reply));
                h.log.info("Connected dpid {} to controller {}",
                        h.sw.getSwitchName(), h.channel.getRemoteAddress());
                h.sw.setConnected(true);
                h.setState(ACTIVE);
            }

        },
        ACTIVE(true) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                    final OFError m) throws IOException {
                h.sw.handleIO(m, h.channel);
            }

            @Override
            void processOFVendor(final ControllerChannelHandler h,
                    final OFVendor m) {
                if (m.getVendor() == OFNiciraVendorData.NX_VENDOR_ID
                        && m.getVendorData() instanceof OFRoleRequestVendorData) {
                    h.sw.handleRoleIO(m, h.channel);
                } else {
                    this.unhandledMessageReceived(h, m);
                }
            }

            @Override
            void processOFMessage(final ControllerChannelHandler h,
                    final OFMessage m) throws IOException {

                switch (m.getType()) {
                case HELLO:
                    this.processOFHello(h, (OFHello) m);
                    break;
                case ECHO_REPLY:
                    break;
                case ECHO_REQUEST:
                    this.processOFEchoRequest(h, (OFEchoRequest) m);
                    break;

                case FEATURES_REQUEST:
                    this.processOFFeaturesRequest(h, (OFFeaturesRequest) m);
                    break;
                case BARRIER_REQUEST:
                    // TODO: actually implement barrier contract
                    final OFBarrierReply breply = new OFBarrierReply();
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
                    h.sw.handleIO(m, h.channel);
                    break;
                case VENDOR:
                    processOFVendor(h, (OFVendor) m);
                    break;
                case FEATURES_REPLY:
                case FLOW_REMOVED:
                case PACKET_IN:
                case PORT_STATUS:
                case BARRIER_REPLY:
                case GET_CONFIG_REPLY:
                case STATS_REPLY:
                case QUEUE_GET_CONFIG_REPLY:
                    this.illegalMessageReceived(h, m);
                    break;
                default:
                    break;
                }
            }

        };

        private boolean handshakeComplete = false;

        ChannelState(final boolean handshakeComplete) {
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
        // needs to be protected because enum members are actually subclasses
        protected String getControllerStateMessage(
                final ControllerChannelHandler h, final OFMessage m,
                final String details) {
            return String.format(
                    "Controller: [%s], State: [%s], received: [%s]"
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
         *             we always through the exception
         */
        // needs to be protected because enum members are actually subclasses
        protected void illegalMessageReceived(final ControllerChannelHandler h,
                final OFMessage m) {
            final String msg = this
                    .getControllerStateMessage(h, m,
                            "Controller should never send this message in the current state");
            throw new ControllerStateException(msg);

        }

        /**
         * We have an OFMessage we didn't expect given the current state and we
         * want to ignore the message.
         *
         * @param h the channel handler the received the message
         * @param m the message
         */
        protected void unhandledMessageReceived(
                final ControllerChannelHandler h, final OFMessage m) {

            if (m.getType() == OFType.VENDOR) {
                h.log.warn(
                        "Received unhandled VENDOR message, sending unsupported error: {}",
                        m);
                OFMessage e = OVXMessageUtil.makeErrorMsg(
                        OFBadRequestCode.OFPBRC_BAD_VENDOR, m);
                h.channel.write(Collections.singletonList(e));
            } else {
                h.log.warn(
                        "Received unhandled message, sending bad type error: {}",
                        m);
                OFMessage e = OVXMessageUtil.makeErrorMsg(
                        OFBadRequestCode.OFPBRC_BAD_TYPE, m);
                h.channel.write(Collections.singletonList(e));
            }
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
        void processOFMessage(final ControllerChannelHandler h,
                final OFMessage m) throws IOException {
            switch (m.getType()) {
            case HELLO:
                this.processOFHello(h, (OFHello) m);
                break;
            case ECHO_REPLY:
                this.processOFEchoReply(h, (OFEchoReply) m);
                break;
            case ECHO_REQUEST:
                this.processOFEchoRequest(h, (OFEchoRequest) m);
                break;
            case ERROR:
                this.processOFError(h, (OFError) m);
                break;
            case VENDOR:
                this.processOFVendor(h, (OFVendor) m);
                break;
            // The following messages are sent to switches. The controller
            // should never receive them
            case SET_CONFIG:
                this.processOFSetConfig(h, (OFSetConfig) m);
                break;
            case PACKET_OUT:
                this.processOFPacketOut(h, (OFPacketOut) m);
                break;
            case PORT_MOD:
                this.processOFPortMod(h, (OFPortMod) m);
                break;
            case QUEUE_GET_CONFIG_REQUEST:
                this.processOFQueueGetConfigRequest(h,
                        (OFQueueGetConfigRequest) m);
                break;
            case BARRIER_REQUEST:
                this.processOFBarrierRequest(h, (OFBarrierRequest) m);
                break;
            case STATS_REQUEST:
                this.processOFStatsRequest(h, (OFStatisticsRequest) m);
                break;
            case FEATURES_REQUEST:
                this.processOFFeaturesRequest(h, (OFFeaturesRequest) m);
                break;
            case FLOW_MOD:
                this.processOFFlowMod(h, (OFFlowMod) m);
                break;
            case GET_CONFIG_REQUEST:
                this.processOFGetConfigRequest(h, (OFGetConfigRequest) m);
                break;

            case FEATURES_REPLY:
            case FLOW_REMOVED:
            case PACKET_IN:
            case PORT_STATUS:
            case BARRIER_REPLY:
            case GET_CONFIG_REPLY:
            case STATS_REPLY:
            case QUEUE_GET_CONFIG_REPLY:
                this.illegalMessageReceived(h, m);
                break;
            default:
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

        void processOFHello(final ControllerChannelHandler h, final OFHello m)
                throws IOException {
            // we only expect hello in the WAIT_HELLO state
            this.illegalMessageReceived(h, m);
        }

        void processOFSetConfig(final ControllerChannelHandler h,
                final OFSetConfig m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFEchoRequest(final ControllerChannelHandler h,
                final OFEchoRequest m) throws IOException {
            final OFEchoReply reply = (OFEchoReply) BasicFactory.getInstance()
                    .getMessage(OFType.ECHO_REPLY);
            reply.setXid(m.getXid());
            reply.setPayload(m.getPayload());
            reply.setLengthU(m.getLengthU());
            h.channel.write(Collections.singletonList(reply));
        }

        void processOFFeaturesRequest(final ControllerChannelHandler h,
                final OFFeaturesRequest m) {
            OFFeaturesReply fr = h.sw.getFeaturesReply();
            fr.setXid(m.getXid());
            h.channel.write(Collections.singletonList(fr));
        }

        void processOFEchoReply(final ControllerChannelHandler h,
                final OFEchoReply m) throws IOException {
            // Do nothing with EchoReplies !!
        }

        // no default implementation for OFError
        // every state must override it
        abstract void processOFError(ControllerChannelHandler h, OFError m)
                throws IOException;

        void processOFPacketOut(final ControllerChannelHandler h,
                final OFPacketOut m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFPortMod(final ControllerChannelHandler h,
                final OFPortMod m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFQueueGetConfigRequest(final ControllerChannelHandler h,
                final OFQueueGetConfigRequest m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFBarrierRequest(final ControllerChannelHandler h,
                final OFBarrierRequest m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFStatsRequest(final ControllerChannelHandler h,
                final OFStatisticsRequest m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFFlowMod(final ControllerChannelHandler h,
                final OFFlowMod m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFGetConfigRequest(final ControllerChannelHandler h,
                final OFGetConfigRequest m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFVendor(final ControllerChannelHandler h, final OFVendor m)
                throws IOException {

            this.unhandledMessageReceived(h, m);
        }

    }

    private ChannelState state;
    private Integer handshakeTransactionIds = -1;

    public ControllerChannelHandler(final OpenVirteXController ctrl,
            final OVXSwitch sw) {
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
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) throws Exception {
        this.channel = e.getChannel();
        this.sendHandShakeMessage(OFType.HELLO);
        this.setState(ChannelState.WAIT_HELLO);
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) throws Exception {

        if (this.sw != null) {
            this.sw.removeChannel(this.channel);
            this.sw.setConnected(false);

        }

    }

    @Override
    protected void sendHandShakeMessage(final OFType type) throws IOException {
        final OFMessage m = BasicFactory.getInstance().getMessage(type);
        m.setXid(this.handshakeTransactionIds--);
        this.channel.write(Collections.singletonList(m));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {

        /*
         * Pass all messages to the handlers, except LLDP which goes to the
         * virtual network handler.
         *
         * This should be implemented with a token bucket in order to rate limit
         * the connections a little.
         */
        if (e.getMessage() instanceof List) {
            @SuppressWarnings("unchecked")
            final List<OFMessage> msglist = (List<OFMessage>) e.getMessage();

            for (final OFMessage ofm : msglist) {

                try {
                    switch (ofm.getType()) {
                    case PACKET_OUT:
                        /*
                         * Is this packet a packet out? If yes is it an lldp?
                         * then send it to the OVXNetwork.
                         */
                        final byte[] data = ((OFPacketOut) ofm).getPacketData();
                        if (data.length >= 14) {
                            final int tenantId = ((OVXSwitch) this.sw)
                                    .getTenantId();
                            if (OVXLLDP.isLLDP(data)) {
                                OVXMap.getInstance()
                                        .getVirtualNetwork(tenantId)
                                        .handleLLDP(ofm, this.sw);
                                break;
                            }
                        }
                    default:
                        // Process all non-packet-ins
                        this.state.processOFMessage(this, ofm);
                        break;
                    }

                } catch (final Exception ex) {
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
    public void channelIdle(final ChannelHandlerContext ctx,
            final IdleStateEvent e) throws Exception {
        final OFMessage m = BasicFactory.getInstance().getMessage(
                OFType.ECHO_REQUEST);
        e.getChannel().write(Collections.singletonList(m));
    }

    /*
     * Set the state for this channel
     */
    private void setState(final ChannelState state) {
        this.state = state;
    }

    public Channel getChannel() {

        return this.channel;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            // switch timeout
            this.log.error("Disconnecting ctrl {} due to read timeout ",
                    this.getSwitchInfoString(), e.getCause());
            ctx.getChannel().close();
        } else if (e.getCause() instanceof HandshakeTimeoutException) {
            this.log.error(
                    "Disconnecting ctrl {} failed to complete handshake ",
                    this.getSwitchInfoString(), e.getCause());
            ctx.getChannel().close();
        } else if (e.getCause() instanceof ClosedChannelException) {
            this.log.error("Channel for ctrl {} already closed",
                    this.getSwitchInfoString(), e.getCause());
        } else if (e.getCause() instanceof IOException) {
            this.log.error("Disconnecting ctrl {} due to IO Error.",
                    this.getSwitchInfoString(), e.getCause());
            ctx.getChannel().close();
        } else if (e.getCause() instanceof SwitchStateException) {
            this.log.error("Disconnecting ctrl {} due to switch state error",
                    this.getSwitchInfoString(), e.getCause());
            ctx.getChannel().close();
        } else if (e.getCause() instanceof MessageParseException) {
            this.log.error(
                    "Disconnecting ctrl {} due to message parse failure",
                    this.getSwitchInfoString(), e.getCause());
            ctx.getChannel().close();
        } else if (e.getCause() instanceof RejectedExecutionException) {
            this.log.error("Could not process message: queue full",
                    e.getCause());

        } else {

            this.log.error(
                    "Error while processing message from switch {} state {}",
                    this.getSwitchInfoString(), this.state, e.getCause());

            ctx.getChannel().close();
            throw new RuntimeException(e.getCause());
        }
    }

}
