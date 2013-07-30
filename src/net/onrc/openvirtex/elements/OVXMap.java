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
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;

public class OVXMap implements Mappable{

    ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>> virtualSwitchMap;
    ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>> physicalSwitchMap;
    ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>> virtualLinkMap;
    ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>> physicalLinkMap;
    ConcurrentHashMap<Integer, OVXNetwork> networkMap; 
    RadixTree<String> ipAddressMap;
    
    private static OVXMap mapInstance = null;
    
    /*
     * constructor for OVXMap will initialize all the dictionaries
     */
    private OVXMap() {
	physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>>();
	virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>();
	physicalLinkMap = new ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>>();
	virtualLinkMap = new ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>();
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
	int tenantId = 0;
	this.physicalSwitchMap.get(physicalSwitch).put(tenantId, virtualSwitch);
	return true;
    }
    
    /**
     * key value pairs from the physicalLink to the virtualLinks which contain it
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    public boolean addPhysicalLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink) {
	int tenantId = 0;
	this.physicalLinkMap.get(physicalLink).put(tenantId, virtualLink);
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
	this.virtualSwitchMap.get(virtualSwitch).add(physicalSwitch);
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
    public boolean addVirtualLinkMapping(OVXLink virtualLink, PhysicalLink physicalLink) {
	this.virtualLinkMap.get(virtualLink).add(physicalLink);
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
	int tenantId = 0;
	this.networkMap.put(tenantId, virtualNetwork);
	return true;
    }
    
    //Access objects from dictionary given the key
    
    /**
     * get the virtualSwitch that are associated with a physicalSwitch
     * which has been specified
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    public OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId) {

	return this.physicalSwitchMap.get(physicalSwitch).get(tenantId);
    }
    
    /**
     * get the virtual source and destination port corresponding to the 
     * physicalLink which is specified
     * 
     * @param physicalLink
     * 
     * @return virtualLinks
     */
    public OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId) {

	return this.physicalLinkMap.get(physicalLink).get(tenantId);
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

	return this.virtualLinkMap.get(virtualLink);
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

	return this.virtualSwitchMap.get(virtualSwitch);
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
	
	return this.networkMap.get(tenantId);
    }
    
    // Remove objects from dictionary
}
