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

package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.core.io.OVXEventHandler;
import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;



public abstract class Switch implements OVXEventHandler, OVXSendMsg {    
    
    protected boolean isConnected = false;
    
    protected OFFeaturesReply featuresReply = null;

    protected Channel channel = null;

    protected OVXDescriptionStatistics desc;
    
    public abstract void handleIO(OFMessage msgs);

    public void setConnected(boolean isConnected) {
	this.isConnected = isConnected;
    }

    public void setFeaturesReply(OFFeaturesReply m) {
	this.featuresReply = m;
    }

    public void setChannel(Channel channel) {
	this.channel  = channel;
	
    }

    public abstract void tearDown();

    public abstract void init();

    public void setDescriptionStats(OVXDescriptionStatistics description) {
	this.desc = description;
	
    }
}
