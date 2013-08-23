/**
 * s
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package net.onrc.openvirtex.core.cmd;

import net.onrc.openvirtex.util.OVXUtil;

import org.kohsuke.args4j.Option;

public class CmdLineSettings {
    public static final String  DEFAULT_CONFIG_FILE      = null;                                    // "config/Config.json";
    public static final String  DEFAULT_OF_HOST          = "0.0.0.0";
    public static final Integer DEFAULT_OF_PORT          = 6633;
    public static final Integer DEFAULT_NUMBER_VIRT_NETS = 255;

    @Option(
	    name = "-cf",
	    aliases = "--configFile",
	    metaVar = "FILE",
	    usage = "OpenVirteX configuration file")
    private final String        configFile               = CmdLineSettings.DEFAULT_CONFIG_FILE;

    @Option(
	    name = "-p",
	    aliases = "--ofPort",
	    metaVar = "INT",
	    usage = "OpenVirteX openflow listen port")
    private final Integer       ofPort                   = CmdLineSettings.DEFAULT_OF_PORT;

    @Option(
	    name = "-h",
	    aliases = "--ofHost",
	    metaVar = "String",
	    usage = "OpenVirteX Host")
    private final String        ofHost                   = CmdLineSettings.DEFAULT_OF_HOST;

    @Option(
	    name = "-n",
	    aliases = "--numVirtual",
	    metaVar = "INT",
	    usage = "The number of virtual networks")
    private final Integer       numVirtual               = CmdLineSettings.DEFAULT_NUMBER_VIRT_NETS;

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
