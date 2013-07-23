/**
 * 
 */
package net.onrc.openvirtex.elements.network;

import java.util.HashMap;

import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.util.MACAddress;

/**
 * @author gerola
 * 
 */
public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {

    // public VLinkManager vLinkMgmt;

    private int                            tenantId;
    private IPAddress                      network;
    private short                          mask;
    private HashMap<IPAddress, MACAddress> gwsMap;

    public int getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final int tenantId) {
	this.tenantId = tenantId;
    }

    public IPAddress getNetwork() {
	return this.network;
    }

    public void setNetwork(final IPAddress network) {
	this.network = network;
    }

    public short getMask() {
	return this.mask;
    }

    public void setMask(final short mask) {
	this.mask = mask;
    }

    public HashMap<IPAddress, MACAddress> getGwsMap() {
	return this.gwsMap;
    }

    public void setGwsMap(final HashMap<IPAddress, MACAddress> gwsMap) {
	this.gwsMap = gwsMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.network.Network#sendLLDP(java.lang.Object)
     */
    @Override
    public void sendLLDP(final OVXSwitch sw) {
	// TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.network.Network#receiveLLDP(java.lang.Object
     * )
     */
    @Override
    public void receiveLLDP(final OVXSwitch sw) {
	// TODO Auto-generated method stub

    }

}