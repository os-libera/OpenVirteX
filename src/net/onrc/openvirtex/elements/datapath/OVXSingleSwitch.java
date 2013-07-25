/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import org.openflow.protocol.OFMessage;

import net.onrc.openvirtex.core.io.OVXSendMsg;
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

	/* (non-Javadoc)
	 * @see net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
	 */
	@Override
	public void sendMsg(OFMessage msg, OVXSendMsg from) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol.OFMessage)
	 */
	@Override
	public void handleIO(OFMessage msgs) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.onrc.openvirtex.elements.datapath.Switch#tearDown()
	 */
	@Override
	public void tearDown() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.onrc.openvirtex.elements.datapath.Switch#init()
	 */
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

}
