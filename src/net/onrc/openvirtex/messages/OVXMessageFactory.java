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

package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.messages.actions.OVXActionDataLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionDataLayerSource;
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
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
* @author alshabib
*
*/
public class OVXMessageFactory extends BasicFactory {

    
    	private static OVXMessageFactory instance = null;
    	
	// not sure how to deal with this...
	// HACK to convert OFMessage* to OVXMessage*
	@SuppressWarnings("rawtypes")
	static final Class convertMap[] = {OVXHello.class, OVXError.class,
			OVXEchoRequest.class, OVXEchoReply.class, OVXVendor.class,
			OVXFeaturesRequest.class, OVXFeaturesReply.class,
			OVXGetConfigRequest.class, OVXGetConfigReply.class,
			OVXSetConfig.class, OVXPacketIn.class, OVXFlowRemoved.class,
			OVXPortStatus.class, OVXPacketOut.class, OVXFlowMod.class,
			OVXPortMod.class, OVXStatisticsRequest.class,
			OVXStatisticsReply.class, OVXBarrierRequest.class,
			OVXBarrierReply.class, OVXQueueGetConfigRequest.class, OVXQueueGetConfigReply.class };

	@SuppressWarnings({ "rawtypes" })
	static final Class convertActionsMap[] = { OVXActionOutput.class,
			OVXActionVirtualLanIdentifier.class,
			OVXActionVirtualLanPriorityCodePoint.class,
			OVXActionStripVirtualLan.class, OVXActionDataLayerSource.class,
			OVXActionDataLayerDestination.class,
			OVXActionNetworkLayerSource.class,
			OVXActionNetworkLayerDestination.class,
			OVXActionNetworkTypeOfService.class,
			OVXActionTransportLayerSource.class,
			OVXActionTransportLayerDestination.class, OVXActionEnqueue.class,
			OVXActionVendor.class };

	@SuppressWarnings("rawtypes")
	static final Class convertStatsRequestMap[] = {
			OVXDescriptionStatistics.class, OVXFlowStatisticsRequest.class,
			OVXAggregateStatisticsRequest.class, OVXTableStatistics.class,
			OVXPortStatisticsRequest.class, OVXQueueStatisticsRequest.class,
			OVXVendorStatistics.class };

	@SuppressWarnings("rawtypes")
	static final Class convertStatsReplyMap[] = {
			OVXDescriptionStatistics.class, OVXFlowStatisticsReply.class,
			OVXAggregateStatisticsReply.class, OVXTableStatistics.class,
			OVXPortStatisticsReply.class, OVXQueueStatisticsReply.class,
			OVXVendorStatistics.class };

	protected OVXMessageFactory() { 
	    super();
	}
	
	public static OVXMessageFactory getInstance() {
		if (instance == null)
		    instance = new OVXMessageFactory();
	        return instance;
	    }
	
	@SuppressWarnings("unchecked")
	@Override
	public OFMessage getMessage(OFType t) {
		if (t == null)
			return new OVXUnknownMessage();
		byte mtype = t.getTypeValue();
		if (mtype >= convertMap.length)
			throw new IllegalArgumentException("OFMessage type " + mtype
					+ " unknown to OVX");
		Class<? extends OFMessage> c = convertMap[mtype];
		try {
			OFMessage m = c.getConstructor(new Class[] {}).newInstance();
			if (m instanceof OFMessageFactoryAware)
				((OFMessageFactoryAware) m).setMessageFactory(this);
			if (m instanceof OFActionFactoryAware) {
				((OFActionFactoryAware) m).setActionFactory(this);
			}
			if (m instanceof OFStatisticsFactoryAware) {
				((OFStatisticsFactoryAware) m).setStatisticsFactory(this);
			}
			return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public OFAction getAction(OFActionType t) {
		Class<? extends OFAction> c = convertActionsMap[t.getTypeValue()];
		try {
			return c.getConstructor(new Class[] {}).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	// big hack; need to fix
	@Override
	public OFStatistics getStatistics(OFType t, OFStatisticsType st) {
		Class<? extends OFStatistics> c;
		if (t == OFType.STATS_REPLY)
			if (st.getTypeValue() == -1)
				c = OVXVendorStatistics.class;
			else
				c = convertStatsReplyMap[st.getTypeValue()];
		else if (t == OFType.STATS_REQUEST)
			if (st.getTypeValue() == -1)
				c = OVXVendorStatistics.class;
			else
				c = convertStatsRequestMap[st.getTypeValue()];
		else
			throw new RuntimeException("non-stats type in stats factory: " + t);
		try {
			return c.getConstructor(new Class[] {}).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

