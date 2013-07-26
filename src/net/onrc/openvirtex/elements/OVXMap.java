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
    
    public OVXMap() {
	/*
	 * constructor for OVXMap will initialize all the dictionaries
	 */
	physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, OVXSwitch>();
	virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, PhysicalSwitch>();
	physicalPortMap = new ConcurrentHashMap<PhysicalPort, OVXPort>();
	virtualPortMap = new ConcurrentHashMap<OVXPort, PhysicalPort>();
	networkMap = new ConcurrentHashMap<Integer, OVXNetwork>();
	ipAddressMap = new ConcurrentRadixTree<String>(new DefaultCharArrayNodeFactory());
    }
    
    // ADD objects to dictionary
    /*
     * sets up the mapping from the physicalSwitch to the virtualSwitch 
     * which has been specified
     */
    public boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch) {

	return true;
    }
    
    /*
     * keep track of each physical port and all the virtual ports that 
     * use the physical port mentioned
     */
    public boolean addPhysicalPortMapping(PhysicalPort physicalPort, OVXPort virtualPort) {

	return true;
    }
    
    /*
     * in order to get the list of virtual links that all have
     * used a specific physical link
     */
    public boolean addPhysicalLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink) {

	return true;
    }
 
    /*
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     */
    public boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch, PhysicalSwitch physicalSwitch) {

	return true;
    }
    
    /*
     * maps the OVXPort object to the physical Port that it refers to
     */
    public boolean addVirtualPortMapping(OVXPort virtualPort, PhysicalPort physicalPort) {

	return true;
    }

    /*
     * maps the virtual source and destination port to the list of
     * physical source and destination ports 
     */
    public boolean addVirtualLinkMapping(OVXLink virtualLink, List <PhysicalLink> physicalLinks) {

	return true;
    }
    
    /*
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     */
    public boolean addVirtualNetworkMapping(OVXNetwork virtualNetwork) {
	
	return true;
    }
    
    //Access objects from dictionary given the key
    
    /*
     * sets up the mapping from the physicalSwitch to the virtualSwitch 
     * which has been specified
     */
    public ArrayList<OVXSwitch> getVirtualSwitches(PhysicalSwitch physicalSwitch) {

	return null;
    }

    /*
     * keep track of each physical port and all the virtual ports that 
     * use the physical port mentioned
     */
    public ArrayList<OVXPort> getVirtualPorts(PhysicalPort physicalPort) {

	return null;
    }

    /*
     * in order to get the list of virtual links that all have
     * used a specific physical link
     */
    public ArrayList<PhysicalLink> getPhysicalLinks(OVXMap virtualLink) {

	return null;
    }
    
    /*
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     */
    public ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch) {

	return null;
    }

    /*
     * maps the OVXPort object to the physical Port that it refers to
     */
    public ArrayList<PhysicalPort> getPhysicalPorts(OVXPort virtualPort) {

	return null;
    }
    
    /*
     * maps the virtual source and destination port to the list of
     * physical source and destination ports 
     */
    public ArrayList<OVXLink> getVirtualLinks(PhysicalLink physicalLink) {

	return null;
    }
    
    /*
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetwork
     */
    public OVXNetwork getVirtualNetwork(int tenantId) {
	
	return null;
    }
    
    // Remove objects from dictionary
}
