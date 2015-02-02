package net.onrc.openvirtex.elements.address;

import java.net.InetAddress;

import net.onrc.openvirtex.elements.port.PhysicalPort;

public class FloatingIPAddress extends OVXIPAddress {

    boolean bidirectional = false;
    PhysicalPort physicalPort;

    public FloatingIPAddress(int tenantId, InetAddress ip, PhysicalPort physicalPort, boolean bidirectional) {
        super(ip.getHostAddress(), tenantId);
        this.bidirectional = bidirectional;
        this.physicalPort = physicalPort;
    }

    public PhysicalPort getPhysicalPort() {
        return physicalPort;
    }
}
