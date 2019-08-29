/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFSetConfig;

public class OVXSetConfig extends OVXMessage implements Devirtualizable {
    private final Logger log = LogManager.getLogger(OVXSetConfig.class.getName());

    public static final short MSL_FULL = (short) 0xffff;
    public static final short MSL_DEFAULT = (short) 0x0080;

    public OVXSetConfig(OFMessage msg) {
        super(msg);
    }

    public OFSetConfig getSetConfig(){
        return (OFSetConfig)this.getOFMessage();
    }

    @Override
    public void devirtualize(final OVXSwitch sw) {
        //this.log.info("devirtualize");

        sw.setMissSendLen((short)this.getSetConfig().getMissSendLen());
        this.log.info("Setting miss send length to {} for OVXSwitch {}",
                (short)this.getSetConfig().getMissSendLen(), sw.getSwitchName());
    }
}
