/**
 * 
 */
package net.onrc.openvirtex.elements.network;

import java.util.HashSet;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;

/**
 * @author gerola
 * 
 */

public class PhysicalNetwork extends
        Network<PhysicalSwitch, PhysicalPort, PhysicalLink> {

    private HashSet<Uplink> uplinkList;

    public HashSet<Uplink> getUplinkList() {
	return this.uplinkList;
    }

    public void setUplinkList(final HashSet<Uplink> uplinkList) {
	this.uplinkList = uplinkList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.network.Network#sendLLDP(java.lang.Object)
     */
    @Override
    public void sendLLDP(final PhysicalSwitch sw) {
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
    public void receiveLLDP(final PhysicalSwitch sw) {
	// TODO Auto-generated method stub

    }

}
