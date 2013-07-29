/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

/**
 * 
 */
package net.onrc.openvirtex.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

public class OVXMap implements Mappable{

    ConcurrentHashMap<OVXSwitch, PhysicalSwitch> virtualSwitchMap;
    ConcurrentHashMap<PhysicalSwitch, OVXSwitch> physicalSwitchMap;
    ConcurrentHashMap<OVXPort, PhysicalPort> virtualPortMap;
    ConcurrentHashMap<PhysicalPort, OVXPort> physicalPortMap;
    ConcurrentHashMap<Integer, OVXNetwork> networkMap; 
    RadixTree<String> ipAddressMap;
    
    private static OVXMap mapInstance = null;
    
    /*
     * constructor for OVXMap will initialize all the dictionaries
     */
    private OVXMap() {
	physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, OVXSwitch>();
	virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, PhysicalSwitch>();
	physicalPortMap = new ConcurrentHashMap<PhysicalPort, OVXPort>();
	virtualPortMap = new ConcurrentHashMap<OVXPort, PhysicalPort>();
	networkMap = new ConcurrentHashMap<Integer, OVXNetwork>();
	ipAddressMap = new ConcurrentRadixTree<String>(new DefaultCharArrayNodeFactory());
    }
    
    /**
     * OVXMap is a singleton class. If object has already been created
     * this should return the existing object rather than creating a new object.
     * 
     * @return ovxMap
     */
    public OVXMap getInstance() {
	if (mapInstance == null) {
	    mapInstance = new OVXMap();
	}
	return mapInstance;
    }
    
    // ADD objects to dictionary
    /**
     * adds the mapping from the physicalSwitch to the virtualSwitch 
     * which has been specified
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     * @return success 
     */
    public boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch) {

	return true;
    }
    
    /**
     * add each physical port and all the virtual ports that 
     * use the physical port mentioned
     * 
     * @param physicalPort
     * @param virtualPort
     * 
     * @return success
     */
    public boolean addPhysicalPortMapping(PhysicalPort physicalPort, OVXPort virtualPort) {

	return true;
    }
    
    /**
     * add virtual links that all have used a specific physical link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    public boolean addPhysicalLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink) {

	return true;
    }
 
    /**
     * add the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     * @param physicalSwitch
     */
    public boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch, PhysicalSwitch physicalSwitch) {

	return true;
    }
    
    /**
     * maps the OVXPort object to the physical Port that it refers to
     * 
     * @param virtualPort
     * @param physicalPort
     * 
     * @return success
     */
    public boolean addVirtualPortMapping(OVXPort virtualPort, PhysicalPort physicalPort) {

	return true;
    }

    /**
     * maps the virtual source and destination port to the list of
     * physical source and destination ports
     * 
     * @param virtualLink
     * @param physicalLink
     * 
     * @return success
     */
    public boolean addVirtualLinkMapping(OVXLink virtualLink, List <PhysicalLink> physicalLinks) {

	return true;
    }
    
    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     * 
     * @param virtualNetwork
     * 
     * @return success
     */
    public boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork) {
	
	return true;
    }
    
    //Access objects from dictionary given the key
    
    /**
     * get the physicalSwitchs that are associated with a virtualSwitch 
     * which has been specified
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    public ArrayList<OVXSwitch> getVirtualSwitches(PhysicalSwitch physicalSwitch) {

	return null;
    }

    /**
     * get all the virtual ports that use the physical port mentioned
     * 
     * @param physicalPort
     * 
     * @return virtualPorts
     */
    public ArrayList<OVXPort> getVirtualPorts(PhysicalPort physicalPort) {

	return null;
    }

    /**
     * get the list of virtual links that all have used a specific 
     * physical link
     * 
     * @param virtualLink
     * 
     * @return physicalLinks
     */
    public ArrayList<PhysicalLink> getPhysicalLinks(OVXMap virtualLink) {

	return null;
    }
    
    /**
     * get the physicalSwitches that are associated with the given
     * virtualSwitch
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    public ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch) {

	return null;
    }

    /**
     * get the physicalPort that the OVXPort object
     * refers to
     * 
     * @param virtualPort
     * 
     * @return physicalPorts
     */
    public ArrayList<PhysicalPort> getPhysicalPorts(OVXPort virtualPort) {

	return null;
    }
    
    /**
     * get the virtual source and destination port corresponding to the 
     * physicalLink which is specified
     * 
     * @param physicalLink
     * 
     * @return virtualLinks
     */
    public ArrayList<OVXLink> getVirtualLinks(PhysicalLink physicalLink) {

	return null;
    }
    
    /**
     * get the virtualNetwork based on the tenantId. Stores each virtual network 
     * in a map indexed by the tenant id
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    public OVXNetwork getVirtualNetwork(int tenantId) {
	
	return null;
    }
    
    // Remove objects from dictionary
}
