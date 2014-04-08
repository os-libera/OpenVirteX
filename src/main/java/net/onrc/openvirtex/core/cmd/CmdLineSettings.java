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
package net.onrc.openvirtex.core.cmd;

import net.onrc.openvirtex.util.OVXUtil;

import org.kohsuke.args4j.Option;

import com.mongodb.ServerAddress;

public class CmdLineSettings {
	public static final String DEFAULT_CONFIG_FILE = "config/config.json";
	public static final String DEFAULT_OF_HOST = "0.0.0.0";
	public static final Integer DEFAULT_OF_PORT = 6633;
	public static final Integer DEFAULT_NUMBER_VIRT_NETS = 255;
	public static final String DEFAULT_DB_HOST = ServerAddress.defaultHost();
	public static final Integer DEFAULT_DB_PORT = ServerAddress.defaultPort();
	public static final Boolean DEFAULT_DB_CLEAR = false;
	public static final Integer DEFAULT_STATS_REFRESH = 30;
	public static final Integer DEFAULT_SERVER_THREADS = 32;
	public static final Integer DEFAULT_CLIENT_THREADS = 32;
	public static final Boolean DEFAULT_USE_BDDP = false;

	@Option(name = "-cf", aliases = "--config-file", metaVar = "FILE", usage = "OpenVirteX configuration file")
	private String configFile = CmdLineSettings.DEFAULT_CONFIG_FILE;

	@Option(name = "-p", aliases = "--of-port", metaVar = "INT", usage = "OpenVirteX OpenFlow listen port")
	private Integer ofPort = CmdLineSettings.DEFAULT_OF_PORT;

	@Option(name = "-h", aliases = "--of-host", metaVar = "String", usage = "OpenVirteX host")
	private String ofHost = CmdLineSettings.DEFAULT_OF_HOST;

	@Option(name = "-n", aliases = "--num-virtual", metaVar = "INT", usage = "The number of virtual networks")
	private Integer numVirtual = CmdLineSettings.DEFAULT_NUMBER_VIRT_NETS;
	
	@Option(name = "-dh", aliases = "--db-host", metaVar = "String", usage = "Database host")
	private String dbHost = CmdLineSettings.DEFAULT_DB_HOST;

	@Option(name = "-dp", aliases = "--db-port", metaVar = "INT", usage = "Database port")
	private Integer dbPort = CmdLineSettings.DEFAULT_DB_PORT;

	@Option(name = "--db-clear", usage = "Clear database")
	private Boolean dbClear = CmdLineSettings.DEFAULT_DB_CLEAR;
	
	@Option(name = "--stats-refresh", usage = "Sets what interval to poll statistics with")
	private Integer statsRefresh = CmdLineSettings.DEFAULT_STATS_REFRESH;
	
	@Option(name = "--ct", aliases = "--client-threads", metaVar = "INT", usage = "Number of threads handles controller connections")
	private Integer clientThreads = CmdLineSettings.DEFAULT_CLIENT_THREADS;
	
	@Option(name = "--st", aliases = "--server-threads", metaVar = "INT", usage = "Number of threads handles switch connections")
	private Integer serverThreads = CmdLineSettings.DEFAULT_CLIENT_THREADS;
	
	@Option(name = "--ub", aliases = "--use-bddp", usage = "Use BDDP for network discovery; only use if you know what you are doing.")
	private Boolean useBDDP = CmdLineSettings.DEFAULT_USE_BDDP;

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

	public String getDBHost() {
		return this.dbHost;
	}
	
	public Integer getDBPort() {
		return this.dbPort;
	}
	
	public Boolean getDBClear() {
		return this.dbClear;
	}
	
	public Integer getStatsRefresh() {
		return this.statsRefresh;
	}
	
	public Integer getClientThreads() {
		return this.clientThreads;
	}
	
	public Integer getServerThreads() {
		return this.serverThreads;
	}
	
	public Boolean getUseBDDP() {
		return this.useBDDP;
	}
	
}
