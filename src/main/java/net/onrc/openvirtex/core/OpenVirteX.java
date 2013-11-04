/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.core;

import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.exceptions.OpenVirteXException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class OpenVirteX {

	public static final String VERSION = "OpenVirteX-0.0.1";
	static Logger log = LogManager.getLogger(OpenVirteX.class.getName());

	/**
	 * @param args
	 */
	public static void main(final String[] args) throws OpenVirteXException {
		final CmdLineSettings settings = new CmdLineSettings();
		final CmdLineParser parser = new CmdLineParser(settings);
		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e) {
			parser.printUsage(System.out);
			System.exit(1);
		}

		final OpenVirteXController ctrl = new OpenVirteXController(
				settings.getConfigFile(), settings.getOFHost(),
				settings.getOFPort(), settings.getNumberOfVirtualNets(),
				settings.getDBHost(), settings.getDBPort(), settings.getDBClear(), settings.getStatsRefresh());
		OpenVirteX.log.info("Starting OpenVirteX...");
		ctrl.run();
	}

}
