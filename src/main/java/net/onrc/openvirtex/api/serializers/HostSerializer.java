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
package net.onrc.openvirtex.api.serializers;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.AddressMappingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Class that contains Serializers for a Host's attributes.
 */
public class HostSerializer {

    protected static Logger log = LogManager.getLogger(HostSerializer.class.getName());

    /**
     * A physical host construct used for differentiating representations
     * TODO if (using = JsonSerializer.class) works, we can do away with this
     */
    public class PhyHost {
        Host ref;

        public PhyHost(Host h) {
            ref = h;
        }
    }

    public Collection<PhyHost> getPhysicalHosts() {
        Collection<PhyHost> hosts = new LinkedList<PhyHost>();
        OVXMap map = OVXMap.getInstance();
        for (OVXNetwork vn : map.listVirtualNetworks().values()) {
            for (Host h : vn.getHosts()) {
                hosts.add(new PhyHost(h));
            }
        }
        return hosts;
    }

    /**
     * Serializer for a Host's attributes in a virtual network.
     */
    public static class OVXHostSerializer extends JsonSerializer<Host> {
        @Override
        public void serialize(final Host host, final JsonGenerator jgen,
                final SerializerProvider sp) throws IOException,
                JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeNumberField(TenantHandler.HOST, host.getHostId());
            jgen.writeStringField(TenantHandler.IFADDR, host.getIp().toSimpleString());
            jgen.writeStringField(TenantHandler.MAC, host.getMac().toString());
            jgen.writeStringField(TenantHandler.VDPID, host.getPort().getParentSwitch()
                    .getSwitchName());
            jgen.writeNumberField(TenantHandler.VPORT, host.getPort().getPortNumber());
            jgen.writeEndObject();
        }
    }

    /**
     * Serializer for a Host's attributes in the physical network.
     */
    public static class PhysicalHostSerializer extends JsonSerializer<PhyHost> {

        @Override
        public void serialize(final PhyHost host, final JsonGenerator jgen,
                final SerializerProvider sp) throws IOException,
                JsonProcessingException {
            /* don't serialize IP address if 0.0.0.0 */
            Host h = host.ref;
            String ipstr = "";
            if (h.getIp().getIp() != 0) {
                try {
                    ipstr = OVXMap.getInstance().getPhysicalIP(
                                    h.getIp(), h.getPort().getTenantId())
                            .toSimpleString();
                } catch (AddressMappingException e) {
                    log.warn("Host IP ({}) isn't mapped to a Physical address.",
                            h.getIp());
                }
            }

            jgen.writeStartObject();
            jgen.writeNumberField("hostId", h.getHostId());
            if (!ipstr.equals("")) {
                jgen.writeStringField("ipAddress", ipstr);
            }
            jgen.writeStringField("mac", h.getMac().toString());
            jgen.writeStringField("dpid", h.getPort().getPhysicalPort()
                    .getParentSwitch().getSwitchName());
            jgen.writeNumberField("port", h.getPort().getPhysicalPortNumber());
            jgen.writeEndObject();
        }
    }

}
