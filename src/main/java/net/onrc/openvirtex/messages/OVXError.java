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
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError;

public class OVXError extends OFError implements Virtualizable, Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXError.class.getName());

    @Override
    public void devirtualize(final OVXSwitch sw) {
        // TODO Auto-generated method stub

    }

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        /*
         * TODO: For now, just report the error. In the future parse them and
         * forward to controller if need be.
         */
        log.error(getErrorString(this));

    }

    /**
     * Get an error string from the OFError.
     *
     * @param error the OpenFlow error
     * @return the error string
     */
    private static String getErrorString(OFError error) {
        // TODO: this really should be OFError.toString. Sigh.
        int etint = 0xffff & error.getErrorType();
        if (etint < 0 || etint >= OFErrorType.values().length) {
            return String.format("Unknown error type %d", etint);
        }
        OFErrorType et = OFErrorType.values()[etint];
        switch (et) {
        case OFPET_HELLO_FAILED:
            OFHelloFailedCode hfc = OFHelloFailedCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, hfc);
        case OFPET_BAD_REQUEST:
            OFBadRequestCode brc = OFBadRequestCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, brc);
        case OFPET_BAD_ACTION:
            OFBadActionCode bac = OFBadActionCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, bac);
        case OFPET_FLOW_MOD_FAILED:
            OFFlowModFailedCode fmfc = OFFlowModFailedCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, fmfc);
        case OFPET_PORT_MOD_FAILED:
            OFPortModFailedCode pmfc = OFPortModFailedCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, pmfc);
        case OFPET_QUEUE_OP_FAILED:
            OFQueueOpFailedCode qofc = OFQueueOpFailedCode.values()[0xffff & error
                    .getErrorCode()];
            return String.format("Error %s %s", et, qofc);
        case OFPET_VENDOR_ERROR:
            // no codes known for vendor error
            return String.format("Error %s", et);
        default:
            break;
        }
        return null;
    }

}
