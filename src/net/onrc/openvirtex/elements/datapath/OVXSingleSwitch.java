/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.port.OVXPort;

/**
 * @author gerola
 * 
 */
public class OVXSingleSwitch extends OVXSwitch {

    /**
     * 
     */
    public OVXSingleSwitch() {
	super();
    }

    /**
     * @param switchName
     * @param switchId
     * @param map
     * @param tenantId
     * @param pktLenght
     */
    public OVXSingleSwitch(final String switchName, final long switchId,
	    final OVXMap map, final int tenantId, final short pktLenght) {
	super(switchName, switchId, map, tenantId, pktLenght);
    }

    @Override
    public OVXPort getPort(final short portNumber) {
	return this.portMap.get(portNumber).getCopy();
    }

    @Override
    public boolean addPort(final OVXPort port) {
	if (this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    @Override
    public boolean updatePort(final OVXPort port) {
	if (!this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    @Override
    public boolean removePort(final short portNumber) {
	if (!this.portMap.containsKey(portNumber)) {
	    return false;
	} else {
	    this.portMap.remove(portNumber);
	    return true;
	}
    }

    @Override
    public boolean initialize() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean sendMsg() {
	// TODO Auto-generated method stub
	return false;
    }

}
