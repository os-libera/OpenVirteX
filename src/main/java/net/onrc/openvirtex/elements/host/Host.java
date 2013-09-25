package net.onrc.openvirtex.elements.host;

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

public class Host {
	private final MACAddress mac;
	private final OVXPort port;

	public Host(final MACAddress mac, final OVXPort port) {
		this.mac = mac;
		this.port = port;
	}

	public MACAddress getMac() {
		return mac;
	}

	public OVXPort getPort() {
		return port;
	}
}
