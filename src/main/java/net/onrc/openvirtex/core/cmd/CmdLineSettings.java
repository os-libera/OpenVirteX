/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.core.cmd;

import net.onrc.openvirtex.util.OVXUtil;

import org.kohsuke.args4j.Option;

public class CmdLineSettings {
	public static final String DEFAULT_CONFIG_FILE = "config/config.json";
	public static final String DEFAULT_OF_HOST = "0.0.0.0";
	public static final Integer DEFAULT_OF_PORT = 6633;
	public static final Integer DEFAULT_NUMBER_VIRT_NETS = 255;

	@Option(name = "-cf", aliases = "--configFile", metaVar = "FILE", usage = "OpenVirteX configuration file")
	private final String configFile = CmdLineSettings.DEFAULT_CONFIG_FILE;

	@Option(name = "-p", aliases = "--ofPort", metaVar = "INT", usage = "OpenVirteX openflow listen port")
	private final Integer ofPort = CmdLineSettings.DEFAULT_OF_PORT;

	@Option(name = "-h", aliases = "--ofHost", metaVar = "String", usage = "OpenVirteX Host")
	private final String ofHost = CmdLineSettings.DEFAULT_OF_HOST;

	@Option(name = "-n", aliases = "--numVirtual", metaVar = "INT", usage = "The number of virtual networks")
	private final Integer numVirtual = CmdLineSettings.DEFAULT_NUMBER_VIRT_NETS;

	public String getConfigFile() {
		return this.configFile;
	}

	public String getOFHost() {
		return this.ofHost;
	}

	public Integer getOFPort() {
		return this.ofPort;
	}

	public Integer getNumberOfVirtualNets() {
		return OVXUtil.NUMBITSNEEDED(this.numVirtual);
	}

}
