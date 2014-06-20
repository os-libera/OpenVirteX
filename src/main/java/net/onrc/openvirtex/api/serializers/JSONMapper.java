package net.onrc.openvirtex.api.serializers;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Provides JSON (de)serialization for the OpenVirteX API.
 */
public class JSONMapper extends ObjectMapper {

    /* 'OVX' */
    private static final long serialVersionUID = 0x4f5658L;

    protected SimpleModule ovxmodule;

    public JSONMapper() {
        super();
        this.ovxmodule = new SimpleModule();
        ovxmodule.addSerializer(OVXSwitch.class, new OVXSwitchSerializer());
        ovxmodule.addSerializer(PhysicalSwitch.class, new PhysicalSwitchSerializer());
        ovxmodule.addSerializer(OVXPort.class, new OVXPortSerializer());
        ovxmodule.addSerializer(PhysicalPort.class, new PhysicalPortSerializer());
        ovxmodule.addSerializer(Host.class, new HostSerializer.OVXHostSerializer());
        ovxmodule.addSerializer(HostSerializer.PhyHost.class, new HostSerializer.PhysicalHostSerializer());
        this.registerModule(this.ovxmodule);
    }

}
