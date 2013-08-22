/**
 * 
 */
package net.onrc.openvirtex.elements.network;

import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class Uplink {

	private PhysicalPort uplinkPort;

	private IPAddress uplinkIp;

	private MACAddress uplinkMac;

	private IPAddress nextHopIp;

	private MACAddress nectHopMac;

	public PhysicalPort getUplinkPort() {
		return this.uplinkPort;
	}

	public void setUplinkPort(final PhysicalPort uplinkPort) {
		this.uplinkPort = uplinkPort;
	}

	public IPAddress getUplinkIp() {
		return this.uplinkIp;
	}

	public void setUplinkIp(final IPAddress uplinkIp) {
		this.uplinkIp = uplinkIp;
	}

	public MACAddress getUplinkMac() {
		return this.uplinkMac;
	}

	public void setUplinkMac(final MACAddress uplinkMac) {
		this.uplinkMac = uplinkMac;
	}

	public IPAddress getNextHopIp() {
		return this.nextHopIp;
	}

	public void setNextHopIp(final IPAddress nextHopIp) {
		this.nextHopIp = nextHopIp;
	}

	public MACAddress getNectHopMac() {
		return this.nectHopMac;
	}

	public void setNectHopMac(final MACAddress nectHopMac) {
		this.nectHopMac = nectHopMac;
	}

}