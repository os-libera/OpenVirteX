package net.onrc.openvirtex.elements.address;

import java.net.InetAddress;

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

public class FloatingIPAddress extends OVXIPAddress {

    boolean bidirectional = false;
    OVXPort ovxPort;

    public FloatingIPAddress(InetAddress ip, OVXPort ovxPort, boolean bidirectional) {
        super(ip.getHostAddress(), ovxPort.getTenantId());
        this.bidirectional = bidirectional;
        this.ovxPort = ovxPort;
    }

    public OVXPort getOvxPort() {
        return ovxPort;
    }
}
