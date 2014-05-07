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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFSetConfig;

public class OVXSetConfig extends OFSetConfig implements Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXSetConfig.class
            .getName());

    /**
     * miss_send_len for a full packet (-1).
     */
    public static final short MSL_FULL = (short) 0xffff;
    /**
     * Default miss_send_len when unspecified.
     */
    public static final short MSL_DEFAULT = (short) 0x0080;

    @Override
    public void devirtualize(final OVXSwitch sw) {

        sw.setMissSendLen(this.missSendLength);
        this.log.info("Setting miss send length to {} for OVXSwitch {}",
                this.missSendLength, sw.getSwitchName());

    }

}
