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
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.HandshakeTimeoutException;
import net.onrc.openvirtex.exceptions.SwitchStateException;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXPortStatus;
import net.onrc.openvirtex.messages.OVXSetConfig;
import net.onrc.openvirtex.messages.statistics.OVXDescStatsReply;
import net.onrc.openvirtex.packet.OVXLLDP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.ReadTimeoutException;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;



public class SwitchChannelHandler extends OFChannelHandler {

    Logger log = LogManager.getLogger(SwitchChannelHandler.class.getName());
    protected ArrayList<OVXPortStatus> pendingPortStatusMsg = null;

    //Indicates the openflow version used by this switch
    protected OFVersion ofVersion;
    protected OFFactory factory;

    private long thisdpid; // channelHandler cached value of connected switch id


    // needs to check if the handshake is complete
    private ChannelState state;

    // Temporary storage for switch-features and port-description
    private OFFeaturesReply featuresReply;
    private List<OFPortDescStatsReply> portDescReplies = new ArrayList<>();

    /*
     * Transaction ids to use during initialization
     */
    private int handshakeTransactionIds = -1;

    public SwitchChannelHandler(final OpenVirteXController ctrl) {
        this.ctrl = ctrl;
        this.state = ChannelState.INIT;
        this.pendingPortStatusMsg = new ArrayList<OVXPortStatus>();
    }

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
            void processOFMessage(SwitchChannelHandler h, OVXMessage m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m)
                    throws IOException {
                // no need to actually implement
                // because it won't happen because nothing
                // is connected to us.

            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                this.unhandledMessageReceived(h, m);
            }

        },
        WAIT_HELLO(false) {

            @Override
            void processOFHello(final SwitchChannelHandler h, final OVXMessage m)
                    throws IOException {
                //h.sendHandShakeMessage(OFType.FEATURES_REQUEST);

                if (m.getOFMessage().getVersion().getWireVersion() >= OFVersion.OF_13.getWireVersion()) {
                    h.log.debug("Received {} Hello from {} - switching to OF "
                                    + "version 1.3", m.getOFMessage().getVersion(),
                            h.channel.getRemoteAddress());

                    h.ofVersion = OFVersion.OF_13;
                    h.factory = OFFactories.getFactory(OFVersion.OF_13);

                    h.sendHandshakeHelloMessage();
                } else if (m.getOFMessage().getVersion().getWireVersion() >= OFVersion.OF_10.getWireVersion()) {
                    h.log.debug("Received {} Hello from {} - switching to OF "
                                    + "version 1.0", m.getOFMessage().getVersion(),
                            h.channel.getRemoteAddress());

                    h.ofVersion = OFVersion.OF_10;
                    h.factory = OFFactories.getFactory(OFVersion.OF_10);

                    /*OFHello hi = h.factory.buildHello()
                                    .setXid(h.handshakeTransactionIds--)
                                    .build();
                    h.channel.write(Collections.singletonList(hi));*/
                    h.sendHandshakeHelloMessage();

                } else {
                    h.log.error("Received Hello of version {} from switch at {}. "
                                    + "This controller works with OF1.0 and OF1.3 "
                                    + "switches. Disconnecting switch ...",
                            m.getOFMessage().getVersion(), h.channel.getRemoteAddress());
                    h.channel.disconnect();
                    return;
                }


                h.sendHandshakeFeaturesRequestMessage(m);
                h.setState(WAIT_FEATURES_REPLY);
            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                this.unhandledMessageReceived(h, m);
            }

            @Override
            void processOFFeaturesReply(SwitchChannelHandler h, OVXMessage  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFStatisticsReply(SwitchChannelHandler h, OVXMessage  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
        },
        WAIT_FEATURES_REPLY(false) {

            @Override
            void processOFFeaturesReply(final SwitchChannelHandler h,
                                        final OVXMessage m) throws IOException {
                h.thisdpid = ((OFFeaturesReply)m.getOFMessage()).getDatapathId().getLong();
                h.log.debug("Received features reply for switch at {} with dpid {}",
                        h.getSwitchInfoString(), h.thisdpid);

                h.featuresReply = (OFFeaturesReply)m.getOFMessage();

                //h.log.info("WAIT_FEATURES_REPLY");
                //h.log.info(h.featuresReply.toString());

                if (h.ofVersion == OFVersion.OF_10) {
                    h.sendHandshakeSetConfig();
                    h.setState(WAIT_CONFIG_REPLY);
                } else {
                    //version is 1.3, must get switch port information
                    h.sendHandshakeOFPortDescRequest();
                    h.setState(WAIT_PORT_DESC_REPLY);
                }

                //h.sendHandshakeSetConfig();
                //h.setState(WAIT_CONFIG_REPLY);
            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m)
                    throws IOException {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                //this.unhandledMessageReceived(h, m);
                h.pendingPortStatusMsg.add((OVXPortStatus)m);
            }

            @Override
            void processOFStatisticsReply(SwitchChannelHandler h, OVXMessage  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
        },

        /**
         * We are waiting for a description of the 1.3 switch ports.
         * Once received, we send a SetConfig request
         * Next State is WAIT_CONFIG_REPLY
         */
        WAIT_PORT_DESC_REPLY(false) {

            @Override
            void processOFStatisticsReply(SwitchChannelHandler h, OVXMessage m)
                    throws SwitchStateException {
                // Read port description
                OFStatsReply thisMsg = (OFStatsReply)m.getOFMessage();

                if (thisMsg.getStatsType() != OFStatsType.PORT_DESC) {
                    h.log.warn("Expecting port description stats but received stats "
                                    + "type {} from {}. Ignoring ...", thisMsg.getStatsType(),
                            h.channel.getRemoteAddress());
                    return;
                }
                if (thisMsg.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                    h.log.debug("Stats reply indicates more stats from sw {} for "
                                    + "port description",
                            h.getSwitchInfoString());
                    h.portDescReplies.add((OFPortDescStatsReply)thisMsg);
                    return;
                }
                else {
                    h.portDescReplies.add((OFPortDescStatsReply)thisMsg);
                }
                //h.portDescReply = (OFPortDescStatsReply) m; // temp store
                h.log.info("Received port desc reply for switch at {}",
                        h.getSwitchInfoString());
                try {
                    h.sendHandshakeSetConfig();
                } catch (IOException e) {
                    h.log.error("Unable to send setConfig after PortDescReply. "
                            + "Error: {}", e.getMessage());
                }
                h.setState(WAIT_CONFIG_REPLY);
            }

            @Override
            void processOFError(SwitchChannelHandler h, OVXMessage m)
                    throws IOException, SwitchStateException {
                logErrorDisconnect(h, m);

            }

            @Override
            void processOFPortStatus(SwitchChannelHandler h, OVXMessage m)
                    throws IOException, SwitchStateException {
                h.pendingPortStatusMsg.add((OVXPortStatus)m);

            }
        },

        /**
         * We are waiting for a config reply message. Once we receive it
         * we send a DescriptionStatsRequest to the switch.
         * Next state: WAIT_DESCRIPTION_STAT_REPLY
         */
        WAIT_CONFIG_REPLY(false) {
            @Override
            void processOFGetConfigReply(final SwitchChannelHandler h,
                                         final OVXMessage m) throws IOException {

                if ((short)(((OFGetConfigReply)m.getOFMessage()).getMissSendLen()) != (short) 0xffff) {
                    h.log.error(
                            "Miss send length was not set properly by switch {}",
                            h.featuresReply.getDatapathId());
                }
                h.sendHandshakeDescriptionStatsRequest();
                h.setState(WAIT_DESCRIPTION_STAT_REPLY);
            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                h.pendingPortStatusMsg.add((OVXPortStatus)m);
            }
        },
        WAIT_DESCRIPTION_STAT_REPLY(false) {

            @Override
            void processOFStatisticsReply(final SwitchChannelHandler h,
                                          final OVXMessage m) {
                // Read description, if it has been updated
                if (((OFStatsReply)m.getOFMessage()).getStatsType() != OFStatsType.DESC) {
                    h.log.warn("Expecting Description stats but received stats "
                            + "type {} from {}. Ignoring ...", ((OFStatsReply)m.getOFMessage()).getStatsType(),
                            h.channel.getRemoteAddress());
                    return;
                }


                OFDescStatsReply drep = (OFDescStatsReply) m.getOFMessage();

                final OVXDescStatsReply description = new OVXDescStatsReply(drep);

                //delete flow table
                OFFlowMod fm = h.factory.buildFlowDelete()
                        .setMatch(h.factory.buildMatch().build())
                        .build();
                h.channel.write(Collections.singletonList(fm));


                h.sw = new PhysicalSwitch(h.featuresReply.getDatapathId().getLong(), h.ofVersion);
                // set switch information
                // set features reply and channel first so we have a DPID and
                // channel info.

                h.sw.setFeaturesReply(h.featuresReply);
                h.sw.setDescriptionStats(description);

                if(h.sw.getOfVersion() == OFVersion.OF_10) {
                    h.sw.setPortDescEntries(h.featuresReply.getPorts());
                }else{
                    h.sw.setPortDescReplies(h.portDescReplies);
                    for(OFPortDescStatsReply reply : h.portDescReplies) {
                        h.sw.setPortDescEntries(reply.getEntries());
                    }
                }

                h.sw.setConnected(true);
                h.sw.setChannel(h.channel);

                for (final OVXPortStatus ps : h.pendingPortStatusMsg) {
                    this.handlePortStatusMessage(h, ps);
                }
                h.pendingPortStatusMsg.clear();
                h.sw.boot();

                 /*final OVXDescriptionStatistics description = new OVXDescriptionStatistics();
                final ChannelBuffer data = ChannelBuffers.buffer(description
                        .getLength());
                final OFStatistics f = m.getFirstStatistics();
                f.writeTo(data);
                description.readFrom(data);
                OFFlowMod fm = new OFFlowMod();
                fm.setCommand(OFFlowMod.OFPFC_DELETE);
                fm.setMatch(new OFMatch());
                h.channel.write(Collections.singletonList(fm));
                h.sw = new PhysicalSwitch(h.featuresReply.getDatapathId());
                // set switch information
                // set features reply and channel first so we have a DPID and
                // channel info.
                h.sw.setFeaturesReply(h.featuresReply);
                h.sw.setDescriptionStats(description);
                h.sw.setConnected(true);
                h.sw.setChannel(h.channel);

                for (final OFPortStatus ps : h.pendingPortStatusMsg) {
                    this.handlePortStatusMessage(h, ps);
                }
                h.pendingPortStatusMsg.clear();
                h.sw.boot();*/
                h.setState(ACTIVE);
            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m)
                    throws IOException {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                h.pendingPortStatusMsg.add((OVXPortStatus)m);

            }

            @Override
            void processOFFeaturesReply(SwitchChannelHandler h, OVXMessage  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
        },
        ACTIVE(true) {

           @Override
            void processOFMessage(final SwitchChannelHandler h,
                                  final OVXMessage m) throws IOException {

                switch (m.getOFMessage().getType()) {
                    case ECHO_REQUEST:
                        this.processOFEchoRequest(h, m);
                        break;
                    case BARRIER_REPLY:
                    case ECHO_REPLY:
                        // do nothing but thank the switch
                        break;
                    case HELLO:
                        //h.sendHandShakeMessage(OFType.FEATURES_REQUEST);
                        h.sendHandshakeFeaturesRequestMessage(m);
                        break;
                    case FEATURES_REPLY:
                        h.featuresReply = (OFFeaturesReply)m.getOFMessage();
                        h.sw.setFeaturesReply(h.featuresReply);
                        break;
                    case ERROR:
                    case FLOW_REMOVED:
                    case GET_CONFIG_REPLY:
                    case PACKET_IN:
                    case PORT_STATUS:
                    case QUEUE_GET_CONFIG_REPLY:
                    case STATS_REPLY:
                    case EXPERIMENTER:
//                    case VENDOR:
                        h.sw.handleIO(m, h.channel);
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
                        this.illegalMessageReceived(h, m);
                        break;
                    default:
                        break;
                }

            }

            @Override
            void processOFError(final SwitchChannelHandler h, final OVXMessage m)
                    throws IOException {
                // should never happen

            }

            @Override
            void processOFPortStatus(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {
                // should never happen

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
        protected String getSwitchStateMessage(final SwitchChannelHandler h,
                                               final OVXMessage m, final String details) {
            return String.format("Switch: [%s], State: [%s], received: [%s]"
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
         *             we always through the execption
         */
        // needs to be protected because enum members are acutally subclasses
        protected void illegalMessageReceived(final SwitchChannelHandler h,
                                              final OVXMessage m) {
            final String msg = this
                    .getSwitchStateMessage(h, m,
                            "Switch should never send this message in the current state");
            throw new SwitchStateException(msg);

        }

        /**
         * Handles an OFMessage we didn't expect given the current state, by
         * ignoring the message.
         *
         * @param h
         *            the channel handler the received the message
         * @param m
         *            the message
         */
        protected void unhandledMessageReceived(final SwitchChannelHandler h,
                                                final OVXMessage m) {
            h.log.warn(this.getSwitchStateMessage(h, m,
                    "Received unhandled message; moving swiftly along..."));
        }

        /**
         * Log an OpenFlow error message from a switch.
         * @param h The switch that sent the error
         * @param error The error message
         */
        protected void logError(SwitchChannelHandler h, OVXMessage error) {
            h.log.error("{} from switch {} in state {}",
                    error.getOFMessage().getClass().getName(),
                    h.getSwitchInfoString(),
                    this.toString());
        }

        /**
         * Log an OpenFlow error message from a switch and disconnect the
         * channel.
         *
         * @param h the IO channel for this switch.
         * @param error The error message
         */
        protected void logErrorDisconnect(SwitchChannelHandler h, OVXMessage error) {
            logError(h, error);
            h.channel.disconnect();
        }

        /**
         * Handle a port status message.
         *
         * Handle a port status message by updating the port maps in a switch
         * instance and notifying Controller about the change so it can dispatch
         * a switch update.
         *
         * @param h
         *            The OFChannelHhandler that received the message
         * @param m
         *            The PortStatus message we received
         *            if true switch port changed events will be dispatched
         */
        protected void handlePortStatusMessage(final SwitchChannelHandler h,
                                               final OVXMessage m) {
            if (h.sw == null) {
                final String msg = this.getSwitchStateMessage(h, m,
                        "State machine error: switch is null. Should never "
                                + "happen");
                throw new SwitchStateException(msg);
            }
            h.sw.handleIO(m, h.channel);
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
         *            The SwitchChannelHandler2 that received the message
         * @param m
         *            The message we received.
         * @throws SwitchStateException
         * @throws IOException
         */
        void processOFMessage(final SwitchChannelHandler h, final OVXMessage m)
                throws IOException {

             switch (m.getOFMessage().getType()) {
                case HELLO:
                    //h.log.info("HELLO");
                    this.processOFHello(h, m);
                    break;
                case BARRIER_REPLY:
                    //h.log.info("BARRIER_REPLY");
                    this.processOFBarrierReply(h, m);
                    break;
                case ECHO_REPLY:
                    //h.log.info("ECHO_REPLY");
                    this.processOFEchoReply(h, m);
                    break;
                case ECHO_REQUEST:
                    //h.log.info("ECHO_REQUEST");
                    this.processOFEchoRequest(h, m);
                    break;
                case ERROR:
                    //h.log.info("ERROR");
                    this.processOFError(h, m);
                    break;
                case FEATURES_REPLY:
                    //h.log.info("FEATURES_REPLY");
                    this.processOFFeaturesReply(h, m);
                    break;
                case FLOW_REMOVED:
                    //h.log.info("FLOW_REMOVED");
                    this.processOFFlowRemoved(h, m);
                    break;
                case GET_CONFIG_REPLY:
                    //h.log.info("GET_CONFIG_REPLY");
                    this.processOFGetConfigReply(h, m);
                    break;
                case PACKET_IN:
                    //h.log.info("PACKET_IN");
                    this.processOFPacketIn(h, m);
                    break;
                case PORT_STATUS:
                    //h.log.info("PORT_STATUS");
                    this.processOFPortStatus(h, m);
                    break;
                case QUEUE_GET_CONFIG_REPLY:
                    //h.log.info("QUEUE_GET_CONFIG_REPLY");
                    this.processOFQueueGetConfigReply(h, m);
                    break;
                case STATS_REPLY:
                    //h.log.info("STATS_REPLY");
                    this.processOFStatisticsReply(h, m);
                    break;
                case EXPERIMENTER:
                    this.processOFExperimenter(h, m);
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
                    this.illegalMessageReceived(h, m);
                    break;
                default:
                    break;
            }
        }

        /**
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
         *
         * @param h the switch channel handler
         * @param m the OpenFlow hello message
         * @throws IOException
         **/
        void processOFHello(final SwitchChannelHandler h, final OVXMessage m)
                throws IOException {
            // we only expect hello in the WAIT_HELLO state
            this.illegalMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow barrier reply message.
         * UNIMPLEMENTED
         *
         * @param h the switch channel handler
         * @param m the the barrier reply message
         * @throws IOException TODO
         */
        void processOFBarrierReply(final SwitchChannelHandler h,
                                   final OVXMessage m) throws IOException {
            // Silently ignore.
            return;
        }

        /**
         * Processes OpenFlow echo request message.
         *
         * @param h the switch channel handler
         * @param m the echo request message
         * @throws IOException TODO
         */
        void processOFEchoRequest(final SwitchChannelHandler h,
                                  final OVXMessage m) throws IOException {
            /*final OFEchoReply reply = (OFEchoReply) BasicFactory.getInstance()
                    .getMessage(OFType.ECHO_REPLY);
            reply.setXid(m.getXid());
            reply.setPayload(m.getPayload());
            reply.setLengthU(m.getLengthU());
            h.channel.write(Collections.singletonList(reply));*/
            OFEchoReply reply = h.factory.buildEchoReply()
                    .setXid(((OFEchoRequest)m.getOFMessage()).getXid())
                    .setData(((OFEchoRequest)m.getOFMessage()).getData())
                    .build();
            h.channel.write(Collections.singletonList(reply));
        }

        /**
         * Processes OpenFlow echo reply.
         *
         * @param h the switch channel handler
         * @param m the echo reply message
         * @throws IOException TODO
         */
        void processOFEchoReply(final SwitchChannelHandler h,
                                final OVXMessage m) throws IOException {
            // Do nothing with EchoReplies !!
        }

        /**
         * Processes OpenFlow error message. We don't have
         * a default implementation for OFError, every state
         * must override it.
         *
         * @param h the switch channel handler
         * @param m the error message
         * @throws IOException TODO
         */
        abstract void processOFError(SwitchChannelHandler h, OVXMessage m)
                throws IOException;

        /**
         * Processes OpenFlow features reply message.
         *
         * @param h the switch channel handler
         * @param m the features reply message
         * @throws IOException TODO
         */
        void processOFFeaturesReply(final SwitchChannelHandler h,
                                    final OVXMessage m) throws IOException {
            this.unhandledMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow flow removed message.
         *
         * @param h the switch channel handler
         * @param m the flow removed message
         * @throws IOException TODO
         */
        void processOFFlowRemoved(final SwitchChannelHandler h,
                                  final OVXMessage m) throws IOException {
            this.unhandledMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow config reply message.
         *
         * @param h the switch channel handler
         * @param m the config reply message
         * @throws IOException TODO
         */
        void processOFGetConfigReply(final SwitchChannelHandler h,
                                     final OVXMessage m) throws IOException {

            this.illegalMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow packet in message.
         *
         * @param h the switch channel handler
         * @param m the packet in message
         * @throws IOException TODO
         */
        void processOFPacketIn(final SwitchChannelHandler h, final OVXMessage m)
                throws IOException {
            this.unhandledMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow port status message.
         * No default implementation, every state needs to handle it.
         *
         * @param h the switch channel handler
         * @param m the port status message
         * @throws IOException TODO
         */
        abstract void processOFPortStatus(SwitchChannelHandler h, OVXMessage m)
                throws IOException;

        /**
         * Processes OpenFlow queue config reply message.
         *
         * @param h the switch channel handler
         * @param m the queue config reply message
         * @throws IOException TODO
         */
        void processOFQueueGetConfigReply(final SwitchChannelHandler h,
                                          final OVXMessage m) throws IOException {
            this.unhandledMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow statistics reply message.
         *
         * @param h the switch channel handler
         * @param m the statistics reply message
         * @throws IOException TODO
         */
        void processOFStatisticsReply(final SwitchChannelHandler h,
                                      final OVXMessage m) throws IOException {
            this.unhandledMessageReceived(h, m);
        }

        /**
         * Processes OpenFlow Experimenter message.
         *
         * @param h the switch channel handler
         * @param m the Experimenter message
         * @throws IOException TODO
         */
        void processOFExperimenter(final SwitchChannelHandler h, final OVXMessage m)
                throws IOException {
            this.unhandledMessageReceived(h, m);
        }
    }

    @Override
    public boolean isHandShakeComplete() {
        return this.state.isHandShakeComplete();
    }

    /**
     * Return. a string describing this switch based on the already available
     * information (DPID and/or remote socket).
     *
     * @return
     */
    @Override
    protected String getSwitchInfoString() {
        if (this.sw != null) {
            return this.sw.toString();
        }
        String channelString;
        if (this.channel == null || this.channel.getRemoteAddress() == null) {
            channelString = "?";
        } else {
            channelString = this.channel.getRemoteAddress().toString();
        }
        String dpidString;
        if (this.featuresReply == null) {
            dpidString = "?";
        } else {
            //dpidString = HexString.toHexString(this.featuresReply.getDatapathId());
            dpidString = this.featuresReply.getDatapathId().toString();

        }
        return String.format("DPID -> %s(%s)", dpidString, channelString);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
                                 final ChannelStateEvent e) throws Exception {
        this.channel = e.getChannel();
        //this.sendHandshakeHelloMessage();
        //this.sendHandShakeMessage(OFType.HELLO);
        this.setState(ChannelState.WAIT_HELLO);
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx,
                                    final ChannelStateEvent e) throws Exception {

        if (this.sw != null) {
            this.sw.setConnected(false);
            this.sw.unregister();
        }

    }

    /**
     * Send a message to the switch using the handshake transactions ids.
     *

     * @throws IOException
     */

    /*@Override
    protected void sendHandShakeMessage(final OFType type) throws IOException {
        final OFMessage m = BasicFactory.getInstance().getMessage(type);
        m.setXid(this.handshakeTransactionIds--);
        this.channel.write(Collections.singletonList(m));
    }*/

    // to send HELLO message to switches
    protected void sendHandshakeHelloMessage() throws IOException {
        // The OF protocol requires us to start things off by sending the highest
        // version of the protocol supported.

        // bitmap represents OF1.0 (ofp_version=0x01) and OF1.3 (ofp_version=0x04)
        // see Sec. 7.5.1 of the OF1.3.4 spec
        /*U32 bitmap = U32.ofRaw(0x00000012);
        OFHelloElem hem = this.factory.buildHelloElemVersionbitmap()
                .setBitmaps(Collections.singletonList(bitmap))
                .build();
        OFMessage.Builder mb = this.factory.buildHello()
                .setXid(this.handshakeTransactionIds--)
                .setElements(Collections.singletonList(hem));*/

//일딴 1.0먼저 디버깅을 위해서 Switch OFVersion에 따라서 같은 버전의 메시지를 보내도록 한다.
        OFHello ofHello = this.factory.buildHello()
                .setXid(this.handshakeTransactionIds--)
                .build();

        this.log.info("Sending OF_13 Hello to {}", channel.getRemoteAddress());

        this.channel.write(Collections.singletonList(ofHello));
    }

    // to send FeaturesRequest after HELLO message
    protected void sendHandshakeFeaturesRequestMessage(final OVXMessage m) throws IOException {
        OFFeaturesRequest freq = this.factory
                .buildFeaturesRequest()
                .setXid(handshakeTransactionIds--)
                .build();

        this.channel.write(Collections.singletonList(freq));
    }

    /**
     * Sends the configuration requests to tell the switch we want full packets.
     *
     * @throws IOException
     */
    private void sendHandshakeSetConfig() throws IOException {
        final List<OFMessage> msglist = new ArrayList<OFMessage>(3);

        // Ensure we receive the full packet via PacketIn
        if(this.featuresReply.getNBuffers() > 0) {
            OFSetConfig sc = this.factory
                    .buildSetConfig()
                    .setMissSendLen(OVXSetConfig.MSL_FULL)
                    .setXid(this.handshakeTransactionIds--)
                    .build();
            msglist.add(sc);
        }

        // Barrier
        OFBarrierRequest br = this.factory
                .buildBarrierRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(br);

        // Verify (need barrier?)
        OFGetConfigRequest gcr = this.factory
                .buildGetConfigRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(gcr);

        this.channel.write(msglist);
    }

    protected void sendHandshakeDescriptionStatsRequest() throws IOException {
        OFDescStatsRequest dreq = this.factory
                .buildDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();

        this.channel.write(Collections.singletonList(dreq));

    }

    @Override
    public void channelIdle(final ChannelHandlerContext ctx,
                            final IdleStateEvent e) throws Exception {
        OFMessage m = this.factory.buildEchoRequest().build();
        log.debug("Sending Echo Request on idle channel: {}",
                e.getChannel().getPipeline().getLast().toString());
        e.getChannel().write(Collections.singletonList(m));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
                                final MessageEvent e) throws Exception {

        /*
         * Pass all messages to the handlers, except LLDP which we send straight
         * to the topology controller.
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
                        case PACKET_IN:
                            //this.log.info("PACKET_IN");

                            OFPacketIn temp = (OFPacketIn)(ofm.getOFMessage());
                            final byte[] data = temp.getData();

                            //this.log.info(temp.getData().toString());

                            if (OVXLLDP.isLLDP(data)) {
                                if (this.sw != null) {
                                    //this.log.info("PACKET_IN - handleLLDP");
                                    PhysicalNetwork.getInstance().handleLLDP(ofm, this.sw);
                                } else {
                                    this.log.warn("Switch has not connected yet; dropping LLDP for now");
                                }
                                break;
                            }

                        default:
                            // Process all non-packet-ins
                            //this.log.info("Not PACKET_IN - " + ofm.toString());
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
            this.log.info("Message is not List");
            Channels.fireExceptionCaught(this.channel, new AssertionError(
                    "Message received from Channel is not a list"));
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
                                final ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            // switch timeout
            this.log.error("Disconnecting switch {} due to read timeout ",
                    this.getSwitchInfoString());

            ctx.getChannel().close();
        } else if (e.getCause() instanceof HandshakeTimeoutException) {
            this.log.error(
                    "Disconnecting switch {} failed to complete handshake ",
                    this.getSwitchInfoString());

            ctx.getChannel().close();
        } else if (e.getCause() instanceof ClosedChannelException) {
            this.log.error(
                    "Channel for sw {} already closed; switch needs to reconnect",
                    this.getSwitchInfoString());

        } else if (e.getCause() instanceof IOException) {
            this.log.error("Disconnecting switch {} due to IO Error.",
                    this.getSwitchInfoString());

            ctx.getChannel().close();
        } else if (e.getCause() instanceof SwitchStateException) {
            this.log.error("Disconnecting switch {} due to switch state error",
                    this.getSwitchInfoString());

            ctx.getChannel().close();
        } else if (e.getCause() instanceof OFParseError) {
            this.log.error(
                    "Disconnecting switch {} due to message parse failure",
                    this.getSwitchInfoString());

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
        this.log.debug(e.getCause());
    }

    private void sendHandshakeOFPortDescRequest() throws IOException {
        // Get port description for 1.3 switch
        OFPortDescStatsRequest preq = this.factory.buildPortDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        this.channel.write(Collections.singletonList(preq));
    }

    /*
     * Set the state for this channel
     */
    private void setState(final ChannelState state) {
        switch(state) {
            case INIT:
                this.log.debug("to INIT State");
                break;
            case WAIT_HELLO:
                this.log.debug("to WAIT_HELLO State");
                break;
            case WAIT_FEATURES_REPLY:
                this.log.debug("to WAIT_FEATURES_REPLY");
                break;
            case WAIT_CONFIG_REPLY:
                this.log.debug("to WAIT_CONFIG_REPLY");
                break;
            case WAIT_DESCRIPTION_STAT_REPLY:
                this.log.debug("to WAIT_DESCRIPTION_STAT_REPLY");
                break;
            case WAIT_PORT_DESC_REPLY:
                this.log.debug("to WAIT_PORT_DESC_REPLY");
                break;
            case ACTIVE:
                this.log.debug("to ACTIVE");
                break;
        }
        this.state = state;
    }

}
