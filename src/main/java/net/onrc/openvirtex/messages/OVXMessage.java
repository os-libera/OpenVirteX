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

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;

public class OVXMessage {
    private OFMessage msg;
    public OFFactory factory;

    public OVXMessage(OFMessage msg) {
        this.msg = msg;

        if(msg != null)
            this.factory = OFFactories.getFactory(msg.getVersion());
    }

    public void setOFMessage(OFMessage msg) {
        this.msg = msg;

        if(msg != null)
            this.factory = OFFactories.getFactory(msg.getVersion());
    }

    public OFMessage getOFMessage() {
        return this.msg;
    }

    @Override
    public int hashCode() {
        final int prime = 97;
        int result = 1;
        result = prime * result
                + (this.msg.getType() == null ? 0 : this.msg.getType().hashCode());
        result = prime * result + this.msg.getVersion().getWireVersion();
        result = prime * result + (int)this.msg.getXid();
        return result;
    }
}
