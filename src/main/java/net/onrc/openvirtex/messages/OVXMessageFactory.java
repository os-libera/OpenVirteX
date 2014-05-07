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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.messages.actions.OVXActionDataLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionDataLayerSource;
import net.onrc.openvirtex.messages.actions.OVXActionEnqueue;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkTypeOfService;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.messages.actions.OVXActionStripVirtualLan;
import net.onrc.openvirtex.messages.actions.OVXActionTransportLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionTransportLayerSource;
import net.onrc.openvirtex.messages.actions.OVXActionVendor;
import net.onrc.openvirtex.messages.actions.OVXActionVirtualLanIdentifier;
import net.onrc.openvirtex.messages.actions.OVXActionVirtualLanPriorityCodePoint;
import net.onrc.openvirtex.messages.statistics.OVXAggregateStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXAggregateStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXPortStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXQueueStatisticsReply;
import net.onrc.openvirtex.messages.statistics.OVXQueueStatisticsRequest;
import net.onrc.openvirtex.messages.statistics.OVXTableStatistics;
import net.onrc.openvirtex.messages.statistics.OVXVendorStatistics;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFActionFactoryAware;
import org.openflow.protocol.factory.OFMessageFactoryAware;
import org.openflow.protocol.factory.OFStatisticsFactoryAware;
import org.openflow.protocol.factory.OFVendorDataFactoryAware;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Singleton class that serves as factory for OVX messages.
 */
public class OVXMessageFactory extends BasicFactory {

    private static OVXMessageFactory instance = null;

    // not sure how to deal with this...
    // HACK to convert OFMessage* to OVXMessage*
    @SuppressWarnings("rawtypes")
    static final Class[] CONVERT_MAP = {OVXHello.class, OVXError.class,
            OVXEchoRequest.class, OVXEchoReply.class, OVXVendor.class,
            OVXFeaturesRequest.class, OVXFeaturesReply.class,
            OVXGetConfigRequest.class, OVXGetConfigReply.class,
            OVXSetConfig.class, OVXPacketIn.class, OVXFlowRemoved.class,
            OVXPortStatus.class, OVXPacketOut.class, OVXFlowMod.class,
            OVXPortMod.class, OVXStatisticsRequest.class,
            OVXStatisticsReply.class, OVXBarrierRequest.class,
            OVXBarrierReply.class, OVXQueueGetConfigRequest.class,
            OVXQueueGetConfigReply.class};

    @SuppressWarnings({ "rawtypes" })
    static final Class[] CONVERT_ACTIONS_MAP = {OVXActionOutput.class,
            OVXActionVirtualLanIdentifier.class,
            OVXActionVirtualLanPriorityCodePoint.class,
            OVXActionStripVirtualLan.class, OVXActionDataLayerSource.class,
            OVXActionDataLayerDestination.class,
            OVXActionNetworkLayerSource.class,
            OVXActionNetworkLayerDestination.class,
            OVXActionNetworkTypeOfService.class,
            OVXActionTransportLayerSource.class,
            OVXActionTransportLayerDestination.class, OVXActionEnqueue.class,
            OVXActionVendor.class};

    @SuppressWarnings("rawtypes")
    static final Class[] CONVERT_STATS_REQUEST_MAP = {
            OVXDescriptionStatistics.class, OVXFlowStatisticsRequest.class,
            OVXAggregateStatisticsRequest.class, OVXTableStatistics.class,
            OVXPortStatisticsRequest.class, OVXQueueStatisticsRequest.class,
            OVXVendorStatistics.class};

    @SuppressWarnings("rawtypes")
    static final Class[] CONVERT_STATS_REPLY_MAP = {
            OVXDescriptionStatistics.class, OVXFlowStatisticsReply.class,
            OVXAggregateStatisticsReply.class, OVXTableStatistics.class,
            OVXPortStatisticsReply.class, OVXQueueStatisticsReply.class,
            OVXVendorStatistics.class};

    protected OVXMessageFactory() {
        super();
    }

    public static OVXMessageFactory getInstance() {
        if (OVXMessageFactory.instance == null) {
            OVXMessageFactory.instance = new OVXMessageFactory();
        }
        return OVXMessageFactory.instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OFMessage getMessage(final OFType t) {
        if (t == null) {
            return new OVXUnknownMessage();
        }
        final byte mtype = t.getTypeValue();
        if (mtype >= OVXMessageFactory.CONVERT_MAP.length) {
            throw new IllegalArgumentException("OFMessage type " + mtype
                    + " unknown to OVX");
        }
        final Class<? extends OFMessage> c = OVXMessageFactory.CONVERT_MAP[mtype];
        try {
            final OFMessage m = c.getConstructor(new Class[] {}).newInstance();
            if (m instanceof OFMessageFactoryAware) {
                ((OFMessageFactoryAware) m).setMessageFactory(this);
            }
            if (m instanceof OFActionFactoryAware) {
                ((OFActionFactoryAware) m).setActionFactory(this);
            }
            if (m instanceof OFStatisticsFactoryAware) {
                ((OFStatisticsFactoryAware) m).setStatisticsFactory(this);
            }
            if (m instanceof OFVendorDataFactoryAware) {
                ((OFVendorDataFactoryAware) m).setVendorDataFactory(this);
            }
            return m;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public OFAction getAction(final OFActionType t) {
        final Class<? extends OFAction> c = OVXMessageFactory.CONVERT_ACTIONS_MAP[t
                .getTypeValue()];
        try {
            return c.getConstructor(new Class[] {}).newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    // big hack; need to fix
    @Override
    public OFStatistics getStatistics(final OFType t, final OFStatisticsType st) {
        Class<? extends OFStatistics> c;
        if (t == OFType.STATS_REPLY) {
            if (st.getTypeValue() == -1) {
                c = OVXVendorStatistics.class;
            } else {
                c = OVXMessageFactory.CONVERT_STATS_REPLY_MAP[st.getTypeValue()];
            }
        } else if (t == OFType.STATS_REQUEST) {
            if (st.getTypeValue() == -1) {
                c = OVXVendorStatistics.class;
            } else {
                c = OVXMessageFactory.CONVERT_STATS_REQUEST_MAP[st.getTypeValue()];
            }
        } else {
            throw new RuntimeException("non-stats type in stats factory: " + t);
        }
        try {
            return c.getConstructor(new Class[] {}).newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
