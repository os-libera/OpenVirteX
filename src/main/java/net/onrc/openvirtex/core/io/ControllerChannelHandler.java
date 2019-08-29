/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
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
import net.onrc.openvirtex.messages.OVXMessage;
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
import org.projectfloodlight.openflow.protocol.*;

//import org.openflow.vendor.nicira.OFNiciraVendorData;
//import org.openflow.vendor.nicira.OFRoleRequestVendorData;

public class ControllerChannelHandler extends OFChannelHandler {

    private final Logger log = LogManager
            .getLogger(ControllerChannelHandler.class.getName());

    enum ChannelState {
        INIT(false) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
                // This should never happen. We haven't connected to anyone
                // yet.

            }

        },
        WAIT_HELLO(false) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
                h.log.error("Error waiting for Hello (type:{})",
                        ((OFErrorMsg)m.getOFMessage()).getErrType());

                h.channel.disconnect();
            }

            @Override
            void processOFHello(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {

                h.setState(WAIT_FT_REQ);

                /*if (m.getVersion() == OFMessage.OFP_VERSION) {
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
                }*/
            }
        },
        WAIT_FT_REQ(false) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
                h.log.error(
                        "Error waiting for Features Request (type:{})",
                        ((OFErrorMsg)m.getOFMessage()).getErrType());

                h.channel.disconnect();

            }

            @Override
            void processOFFeaturesRequest(final ControllerChannelHandler h,
                                          final OVXMessage m) {
                OFFeaturesReply reply = h.sw.getFeaturesReply();
                if (h.sw.getFeaturesReply() == null) {
                    h.log.error("OVXSwitch failed to return a featuresReply message: {}"
                            + h.sw.getSwitchName());
                    h.channel.disconnect();
                }

                /*h.sw.setFeaturesReply(h.sw.getFeaturesReply().createBuilder()
                        .setXid(m.getOFMessage().getXid()).build()
                );*/

                //h.log.info("Before Xid = " + reply.getXid());
                reply = reply.createBuilder().setXid(m.getOFMessage().getXid()).build();
                h.sw.setFeaturesReply(reply);
                //h.log.info(" After Xid = " + reply.getXid());


                h.channel.write(Collections.singletonList(reply));
                h.log.info("Connected dpid {} to controller {}",
                        h.sw.getSwitchName(), h.channel.getRemoteAddress());
                h.sw.setConnected(true);

                if(m.getOFMessage().getVersion() == OFVersion.OF_10) {
                    h.setState(ACTIVE);
                }else{
                    h.setState(WAIT_PD_REQ);
                }
            }
        },

        //for OFVersion.OF_13
        WAIT_PD_REQ(false) {
            @Override
            void processOFStatsRequest(final ControllerChannelHandler h,
                                          final OVXMessage m) {

                OFPortDescStatsReply reply = h.sw.getPortDescStatsReply();

                if (h.sw.getPortDescStatsReply() == null) {
                    h.log.error("OVXSwitch failed to return a portDescReply message: {}"
                            + h.sw.getSwitchName());
                    h.channel.disconnect();
                }

                reply = reply.createBuilder().setXid(m.getOFMessage().getXid()).build();
                h.sw.setPortDescReply(reply);

                h.channel.write(Collections.singletonList(reply));
                h.log.info("Send Port Descriptions to dpid {}", h.channel.getRemoteAddress());

                h.setState(ACTIVE);
            }

            @Override
            void processOFError(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
                h.log.error(
                        "Error waiting for Port Description (type:{})",
                        ((OFErrorMsg)m.getOFMessage()).getErrType());

                h.channel.disconnect();
            }
        },


        ACTIVE(true) {

            @Override
            void processOFError(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
                h.sw.handleIO(m, h.channel);
            }

            @Override
            void processOFVendor(final ControllerChannelHandler h,
                                 final OVXMessage m) {

                h.log.info("ACTIVE.processOFVendor");
                h.log.info(m.getOFMessage().toString());


                /*if (m.getVendor() == OFNiciraVendorData.NX_VENDOR_ID
                        && m.getVendorData() instanceof OFRoleRequestVendorData) {
                    h.sw.handleRoleIO(m, h.channel);
                } else {
                    this.unhandledMessageReceived(h, m);
                }*/
            }

            @Override
            void processOFRoleRequest(final ControllerChannelHandler h,
                                      final OVXMessage m)
                    throws IOException {

                h.sw.handleRoleIO(m, h.channel);
            }

            @Override
            void processOFMessage(final ControllerChannelHandler h,
                                  final OVXMessage m) throws IOException {

                switch (m.getOFMessage().getType()) {
                    case HELLO:
                        this.processOFHello(h, m);
                        break;
                    case ECHO_REPLY:
                        break;
                    case ECHO_REQUEST:
                        this.processOFEchoRequest(h, m);
                        break;

                    case FEATURES_REQUEST:
                        this.processOFFeaturesRequest(h, m);
                        break;
                    case BARRIER_REQUEST:
                        // TODO: actually implement barrier contract
                        final OFBarrierReply ofBarrierReply = OFFactories.getFactory(m.getOFMessage().getVersion())
                                .buildBarrierReply()
                                .setXid(m.getOFMessage().getXid())
                                .build();


                        h.channel.write(Collections.singletonList(ofBarrierReply));
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
                    case EXPERIMENTER:
                        processOFVendor(h, m);
                        break;
                    case ROLE_REQUEST:
                        processOFRoleRequest(h, m);
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
                final ControllerChannelHandler h, final OVXMessage m,
                final String details) {
            return String.format(
                    "Controller: [%s], State: [%s], received: [%s]"
                            + ", details: %s", h.getSwitchInfoString(),
                    this.toString(), m.getOFMessage().getType().toString(), details);
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
         * @throws    ControllerStateException
         *             we always through the exception
         */
        // needs to be protected because enum members are actually subclasses
        protected void illegalMessageReceived(final ControllerChannelHandler h,
                                              final OVXMessage m) {
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
                final ControllerChannelHandler h, final OVXMessage m) {

            if (m.getOFMessage().getType() == OFType.EXPERIMENTER) {
                h.log.warn(
                        "Received unhandled VENDOR message, sending unsupported error: {}",
                        m);
                OVXMessage e = OVXMessageUtil.makeErrorMsg(
                        OFBadRequestCode.BAD_EXPERIMENTER, m);
                h.channel.write(Collections.singletonList(e.getOFMessage()));
            } else {
                h.log.warn(
                        "Received unhandled message, sending bad type error: {}",
                        m);
                OVXMessage e = OVXMessageUtil.makeErrorMsg(
                        OFBadRequestCode.BAD_TYPE, m);
                h.channel.write(Collections.singletonList(e.getOFMessage()));
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
                              final OVXMessage m) throws IOException {

            //h.log.info("processOFMessage");
            //h.log.info(m.getOFMessage().toString());

            switch (m.getOFMessage().getType()) {
                case HELLO:
                    this.processOFHello(h, m);
                    break;
                case ECHO_REPLY:
                    this.processOFEchoReply(h, m);
                    break;
                case ECHO_REQUEST:
                    this.processOFEchoRequest(h, m);
                    break;
                case ERROR:
                    this.processOFError(h, m);
                    break;
                case EXPERIMENTER:
                    h.log.info("ControllerChannelHandler2.processOFMessage");
                    h.log.info(m.getOFMessage().toString());

                    this.processOFVendor(h,  m);
                    break;
                case ROLE_REQUEST:
                    this.processOFRoleRequest(h, m);
                    break;
                // The following messages are sent to switches. The controller
                // should never receive them
                case SET_CONFIG:
                    this.processOFSetConfig(h, m);
                    break;
                case PACKET_OUT:
                    this.processOFPacketOut(h, m);
                    break;
                case PORT_MOD:
                    this.processOFPortMod(h, m);
                    break;
                case QUEUE_GET_CONFIG_REQUEST:
                    this.processOFQueueGetConfigRequest(h, m);
                    break;
                case BARRIER_REQUEST:
                    this.processOFBarrierRequest(h, m);
                    break;
                case STATS_REQUEST:
                    this.processOFStatsRequest(h, m);
                    break;
                case FEATURES_REQUEST:
                    this.processOFFeaturesRequest(h, m);
                    break;
                case FLOW_MOD:
                    this.processOFFlowMod(h, m);
                    break;
                case GET_CONFIG_REQUEST:
                    this.processOFGetConfigRequest(h, m);
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

        void processOFHello(final ControllerChannelHandler h, final OVXMessage m)
                throws IOException {
            // we only expect hello in the WAIT_HELLO state
            this.illegalMessageReceived(h, m);
        }

        void processOFSetConfig(final ControllerChannelHandler h,
                                final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFEchoRequest(final ControllerChannelHandler h,
                                  final OVXMessage m) throws IOException {

            final OFEchoReply reply = OFFactories.getFactory(m.getOFMessage().getVersion()).buildEchoReply()
                    .setXid(m.getOFMessage().getXid())
                    .setData(((OFEchoRequest)m.getOFMessage()).getData())
                    .build();

            h.channel.write(Collections.singletonList(reply));
        }

        void processOFFeaturesRequest(final ControllerChannelHandler h,
                                      final OVXMessage m) {
            OFFeaturesReply fr = h.sw.getFeaturesReply();
            fr = fr.createBuilder().setXid(m.getOFMessage().getXid()).build();
            h.channel.write(Collections.singletonList(fr));
        }

        void processOFEchoReply(final ControllerChannelHandler h,
                                final OVXMessage m) throws IOException {
            // Do nothing with EchoReplies !!
        }

        // no default implementation for OFError
        // every state must override it
        abstract void processOFError(ControllerChannelHandler h, OVXMessage m)
                throws IOException;

        void processOFPacketOut(final ControllerChannelHandler h,
                                final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFPortMod(final ControllerChannelHandler h,
                              final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFQueueGetConfigRequest(final ControllerChannelHandler h,
                                            final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFBarrierRequest(final ControllerChannelHandler h,
                                     final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFStatsRequest(final ControllerChannelHandler h,
                                   final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFFlowMod(final ControllerChannelHandler h,
                              final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFGetConfigRequest(final ControllerChannelHandler h,
                                       final OVXMessage m) {
            this.illegalMessageReceived(h, m);
        }

        void processOFVendor(final ControllerChannelHandler h, final OVXMessage m)
                throws IOException {

            this.unhandledMessageReceived(h, m);
        }


        void processOFRoleRequest(final ControllerChannelHandler h, final OVXMessage m)
                throws IOException {

            this.unhandledMessageReceived(h, m);
        }

    }

    private ChannelState state;
    private Integer handshakeTransactionIds = -1;
    private OFFactory ofFactory;

    public ControllerChannelHandler(final OpenVirteXController ctrl,
                                    final OVXSwitch sw) {
        this.ctrl = ctrl;
        this.state = ChannelState.INIT;
        this.sw = sw;

        this.ofFactory = OFFactories.getFactory(this.sw.getOfVersion());
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
        //this.sendHandShakeMessage(OFType.HELLO);
        this.sendHandshakeHelloMessage();
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

    protected void sendHandshakeHelloMessage() throws IOException {
        OFHello ofHello = this.ofFactory.buildHello()
                .setXid(this.handshakeTransactionIds--)
                .build();
        this.channel.write(Collections.singletonList(ofHello));
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
            final List<OVXMessage> msglist = (List<OVXMessage>) e.getMessage();

            for (final OVXMessage ofm : msglist) {

                try {
                    switch (ofm.getOFMessage().getType()) {
                        case PACKET_OUT:
                        /*
                         * Is this packet a packet out? If yes is it an lldp?
                         * then send it to the OVXNetwork.
                         */
                            final byte[] data = ((OFPacketOut) ofm.getOFMessage()).getData();
                            if (data.length >= 14) {

                                final int tenantId = ((OVXSwitch) this.sw)
                                        .getTenantId();

                                if (OVXLLDP.isLLDP(data)) {
//                                    this.log.info("tenantId = " + tenantId);

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

        OFEchoRequest m = this.ofFactory.buildEchoRequest().build();

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
