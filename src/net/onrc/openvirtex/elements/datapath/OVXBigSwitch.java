/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.HashMap;
import java.util.LinkedList;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.routing.RoutingAlgorithms;

/**
 * The Class OVXBigSwitch.
 * 
 * @author gerola
 */

public class OVXBigSwitch extends OVXSwitch {

    /** The alg. */
    private RoutingAlgorithms                                                  alg;

    /** The path map. */
    private final HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>> pathMap;

    /**
     * Instantiates a new oVX big switch.
     */
    public OVXBigSwitch() {
	super();
	this.alg = RoutingAlgorithms.NONE;
	this.pathMap = new HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>>();
    }

    /**
     * Instantiates a new oVX big switch.
     * 
     * @param switchName
     *            the switch name
     * @param switchId
     *            the switch id
     * @param map
     *            the map
     * @param tenantId
     *            the tenant id
     * @param pktLenght
     *            the pkt lenght
     * @param alg
     *            the alg
     */
    public OVXBigSwitch(final String switchName, final long switchId,
	    final OVXMap map, final int tenantId, final short pktLenght,
	    final RoutingAlgorithms alg) {
	super(switchName, switchId, map, tenantId, pktLenght);
	this.alg = alg;
	this.pathMap = new HashMap<OVXPort, HashMap<OVXPort, LinkedList<PhysicalLink>>>();
    }

    /**
     * Gets the alg.
     * 
     * @return the alg
     */
    public RoutingAlgorithms getAlg() {
	return this.alg;
    }

    /**
     * Sets the alg.
     * 
     * @param alg
     *            the new alg
     */
    public void setAlg(final RoutingAlgorithms alg) {
	this.alg = alg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#getPort(short)
     */
    @Override
    public OVXPort getPort(final short portNumber) {
	return this.portMap.get(portNumber).getCopy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#addPort(java.lang.Object)
     */
    @Override
    public boolean addPort(final OVXPort port) {
	if (this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#updatePort(java.lang.Object)
     */
    @Override
    public boolean updatePort(final OVXPort port) {
	if (!this.portMap.containsKey(port.getPortNumber())) {
	    return false;
	} else {
	    this.portMap.put(port.getPortNumber(), port);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#removePort(short)
     */
    @Override
    public boolean removePort(final short portNumber) {
	if (!this.portMap.containsKey(portNumber)) {
	    return false;
	} else {
	    this.portMap.remove(portNumber);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#initialize()
     */
    @Override
    public boolean initialize() {
	// TODO Auto-generated method stub

	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.onrc.openvirtex.elements.datapath.Switch#sendMsg()
     */
    @Override
    public boolean sendMsg() {
	// TODO Auto-generated method stub
	return false;
    }

}
