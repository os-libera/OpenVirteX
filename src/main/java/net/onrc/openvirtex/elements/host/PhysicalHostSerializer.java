package net.onrc.openvirtex.elements.host;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.AddressMappingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializer for a Host's attributes in the physical network.
 */
public class PhysicalHostSerializer extends JsonSerializer<Host> {

    private static Logger log = LogManager.getLogger(PhysicalHostSerializer.class.getName());

    @Override
    public void serialize(final Host host, final JsonGenerator jgen,
            final SerializerProvider sp) throws IOException,
            JsonProcessingException {
        /* don't serialize IP address if 0.0.0.0 */
        String ipstr = "";
        if (host.getIp().getIp() != 0) {
            try {
                ipstr = OVXMap.getInstance().getPhysicalIP(
                                host.getIp(), host.getPort().getTenantId())
                        .toSimpleString();
            } catch (AddressMappingException e) {
                log.warn("Host IP ({}) isn't mapped to a Physical address.",
                        host.getIp()); 
            }
        }

        jgen.writeStartObject();
        jgen.writeNumberField("hostId", host.getHostId());
        if (!ipstr.equals("")) {
            jgen.writeStringField("ipAddress", ipstr);
        }
        jgen.writeStringField("mac", host.getMac().toString());
        jgen.writeStringField("dpid", host.getPort().getPhysicalPort()
                .getParentSwitch().getSwitchName());
        jgen.writeNumberField("port", host.getPort().getPhysicalPortNumber());
        jgen.writeEndObject();

    }

}
