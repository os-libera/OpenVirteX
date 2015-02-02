package net.onrc.openvirtex.routing.nat;

import net.onrc.openvirtex.elements.address.OVXIPAddress;

public class NatTuple {

    private OVXIPAddress srcAddress;
    private Short srcPort;

    public NatTuple(OVXIPAddress srcAddress, Short srcPort) {
        this.srcAddress = srcAddress;
        this.srcPort = srcPort;
    }

    public OVXIPAddress getSrcAddress() {
        return srcAddress;
    }

    public Short getSrcPort() {
        return srcPort;
    }


}
