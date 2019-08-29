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
package net.onrc.openvirtex.messages.actions;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.types.IPv4Address;

import java.util.List;

public class OVXActionSetNwDst extends OVXAction implements VirtualizableAction {

    private OFActionSetNwDst ofActionSetNwDst;

    public OVXActionSetNwDst(OFAction ofAction)
    {
        super(ofAction);
        this.ofActionSetNwDst = (OFActionSetNwDst)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {
        this.ofActionSetNwDst = this.ofActionSetNwDst.createBuilder()
                .setNwAddr(IPv4Address.of(
                        IPMapper.getPhysicalIp(
                                sw.getTenantId(),
                                this.ofActionSetNwDst.getNwAddr().getInt())
                        )
                )
                .build();

        approvedActions.add(this.ofActionSetNwDst);
    }

    @Override
    public int hashCode() {
        return this.getAction().hashCode();
    }
}
