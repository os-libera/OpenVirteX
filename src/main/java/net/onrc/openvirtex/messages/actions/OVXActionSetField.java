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
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.OFBadActionCode;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.List;

/**
 * Created by Administrator on 2016-06-20.
 */
public class OVXActionSetField extends OVXAction implements VirtualizableAction {
    private OFActionSetField ofActionSetField;
    public OVXActionSetField(OFAction ofAction) {
        super(ofAction);
        this.ofActionSetField = (OFActionSetField)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {

        if(ofActionSetField.getField() == MatchField.ETH_SRC ||
                ofActionSetField.getField() == MatchField.ETH_DST) {
            DoVirtualizeDlAddress(sw, approvedActions);
        } if(ofActionSetField.getField() == MatchField.IPV4_SRC) {
            DoVirtualizeNwSrc(sw, approvedActions);
        } if(ofActionSetField.getField() == MatchField.IPV4_DST) {
            DoVirtualizeNwDst(sw, approvedActions);
        } else {
            DoVirtualizeDefault(sw, approvedActions);
        }
    }

    public void DoVirtualizeDlAddress(OVXSwitch sw, List<OFAction> approvedActions) throws ActionVirtualizationDenied {
        final int tid;
        MacAddress mac = (MacAddress)this.ofActionSetField.getField().getValue();
        try {
            tid = sw.getMap().getMAC(mac);
            if (tid != sw.getTenantId()) {
                throw new ActionVirtualizationDenied("Target mac " + mac
                        + " is not in virtual network " + sw.getTenantId(),
                        OFBadActionCode.EPERM);
            }
            approvedActions.add(this.ofActionSetField);
        } catch (AddressMappingException e) {
            throw new ActionVirtualizationDenied("Target mac " + mac
                    + " is not in virtual network " + sw.getTenantId(),
                    OFBadActionCode.EPERM);
        } catch (ActionVirtualizationDenied actionVirtualizationDenied) {
            actionVirtualizationDenied.printStackTrace();
        }
    }

    public void DoVirtualizeNwSrc(OVXSwitch sw, List<OFAction> approvedActions) throws ActionVirtualizationDenied {

        IPv4Address src = (IPv4Address)this.ofActionSetField.getField().getValue();

        this.ofActionSetField = this.ofActionSetField.createBuilder()
                .setField(factory.oxms().ipv4Src(
                        IPv4Address.of(
                                IPMapper.getPhysicalIp(
                                        sw.getTenantId(),
                                        src.getInt())
                                )))
                .build();
        approvedActions.add(this.ofActionSetField);
    }

    public void DoVirtualizeNwDst(OVXSwitch sw, List<OFAction> approvedActions) throws ActionVirtualizationDenied {

        IPv4Address src = (IPv4Address)this.ofActionSetField.getField().getValue();

        this.ofActionSetField = this.ofActionSetField.createBuilder()
                .setField(factory.oxms().ipv4Dst(
                        IPv4Address.of(
                                IPMapper.getPhysicalIp(
                                        sw.getTenantId(),
                                        src.getInt())
                        )))
                .build();
        approvedActions.add(this.ofActionSetField);
    }

    public void DoVirtualizeDefault(OVXSwitch sw, List<OFAction> approvedActions) throws ActionVirtualizationDenied {
        approvedActions.add(this.ofActionSetField);
    }
}
