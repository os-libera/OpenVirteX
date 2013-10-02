/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.core.io;

import java.util.concurrent.ThreadPoolExecutor;

import net.onrc.openvirtex.core.OpenVirteXController;

import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.Timer;

public abstract class OpenflowChannelPipeline implements
		ChannelPipelineFactory, ExternalResourceReleasable {
	protected OpenVirteXController ctrl;
	protected ThreadPoolExecutor pipelineExecutor;
	protected Timer timer;
	protected IdleStateHandler idleHandler;
	protected ReadTimeoutHandler readTimeoutHandler;

	@Override
	public void releaseExternalResources() {
		this.timer.stop();
	}
}
