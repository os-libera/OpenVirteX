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
package net.onrc.openvirtex.elements.datapath;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

public class TestChannelClass implements Channel{
	public boolean msgSent =false;
	public boolean isConnected = false;

	public TestChannelClass(){
		this.bind(new InetSocketAddress("localhost",6634));
		this.isConnected = true;
		this.msgSent = false;

	}
	@Override
	public int compareTo(Channel o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Integer getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelPipeline getPipeline() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return this.isConnected;
	}

	@Override
	public boolean isBound() {
		//For test: if msgSent or not
		return this.msgSent;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return this.isConnected;
	}

	@Override
	public SocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		if(this.isConnected)
			return new InetSocketAddress("localhost",6634);
		else
			return new InetSocketAddress("0.0.0.0",0);
	}

	@Override
	public ChannelFuture write(Object message) {
		this.msgSent = true;
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture disconnect() {
		// TODO Auto-generated method stub
		this.bind(null);
		this.isConnected = false;
		return null;
	}

	@Override
	public ChannelFuture unbind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture close() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture getCloseFuture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInterestOps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isReadable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWritable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ChannelFuture setInterestOps(int interestOps) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture setReadable(boolean readable) {
		// TODO Auto-generated method stub
		return null;
	}

}