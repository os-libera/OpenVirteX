package net.onrc.openvirtex.elements.host;

import net.onrc.openvirtex.elements.address.FloatingIPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

public class NATGateway extends Host {

    // External IP Address
    FloatingIPAddress floatingIPAddress;
    // Internal IP Address (redundant)
    OVXIPAddress virtualIPAddress;

    public NATGateway(MACAddress mac, OVXPort port, Integer hostId) {
        super(mac, port, hostId);
    }

    public void setFloatingIPAddress(FloatingIPAddress floatingIPAddress) {
        this.floatingIPAddress = floatingIPAddress;
    }

    public FloatingIPAddress getFloatingIPAddress() {
        return floatingIPAddress;
    }

    public void setVirtualIPAddress(OVXIPAddress virtualIPAddress) {
        this.virtualIPAddress = virtualIPAddress;
        super.setIPAddress(virtualIPAddress.getIp());
    }

    public OVXIPAddress getVirtualIPAddress() {
        return virtualIPAddress;
    }
}
