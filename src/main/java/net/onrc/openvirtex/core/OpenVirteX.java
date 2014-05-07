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
package net.onrc.openvirtex.core;

import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import net.onrc.openvirtex.exceptions.OpenVirteXException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public final class OpenVirteX {

    public static final String VERSION = "OpenVirteX-0.0.1";
    private static Logger log = LogManager.getLogger(OpenVirteX.class.getName());

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private OpenVirteX() {
    }

    /**
     * Main method to start the OVX controller. Parses command line arguments.
     *
     * @param args string of command line parameters
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

        final OpenVirteXController ctrl = new OpenVirteXController(settings);
        OpenVirteX.log.info("Starting OpenVirteX...");
        ctrl.run();
    }

}
