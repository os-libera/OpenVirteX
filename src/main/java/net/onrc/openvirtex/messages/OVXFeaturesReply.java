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

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;

public class OVXFeaturesReply extends OVXMessage implements Virtualizable {
    private final Logger log = LogManager.getLogger(OVXFeaturesReply.class.getName());

    public OVXFeaturesReply(OFMessage msg) {

        super(msg);
    }

    public OFFeaturesReply getFeaturesReply() {
        return (OFFeaturesReply)this.getOFMessage();
    }

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        // TODO: Log error, we should never receive this message here

        //this.log.info("virtualize");
        return;
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
