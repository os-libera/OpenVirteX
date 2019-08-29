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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.OFBadActionCode;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.List;

public class OVXActionSetDlDst extends OVXAction implements VirtualizableAction {

    private OFActionSetDlDst ofActionSetDlDst;

    private OVXActionSetDlDst(OFAction ofAction) {
        super(ofAction);
        this.ofActionSetDlDst = (OFActionSetDlDst)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {
        final MacAddress mac = this.ofActionSetDlDst.getDlAddr();

        final int tid;
        try {
            tid = sw.getMap().getMAC(mac);
            if (tid != sw.getTenantId()) {
                throw new ActionVirtualizationDenied("Target mac " + mac
                        + " is not in virtual network " + sw.getTenantId(),
                        OFBadActionCode.EPERM);
            }
            approvedActions.add(this.ofActionSetDlDst);
        } catch (AddressMappingException e) {
            throw new ActionVirtualizationDenied("Target mac " + mac
                    + " is not in virtual network " + sw.getTenantId(),
                    OFBadActionCode.EPERM);
        }
    }

    @Override
    public int hashCode() {
        return this.getAction().hashCode();
    }
}
