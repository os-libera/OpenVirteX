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
    
    /**
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
     * getInstance will get the instance of the class and if this already exists
     * then the existing object will be returned. This is a singleton class.
     * 
     * @return OVXMap
     */
    public OVXMap getInstance() {
	if (mapInstance == null) {
	    mapInstance = new OVXMap();
	}
	return mapInstance;
    }
    
    // ADD objects to dictionary
    
    /**
     * create the mapping between virtual switch to physical switch and from 
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     * @return success
     */
    public boolean addSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch) {
	this.addPhysicalSwitchMapping(physicalSwitch, virtualSwitch);
	this.addVirtualSwitchMapping(virtualSwitch, physicalSwitch);
	return true;
	
    }
    
    /**
     * create the mapping between the virtual link to phsyical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    public boolean addLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink) {
	this.addPhysicalLinkMapping(physicalLink, virtualLink);
	this.addVirtualLinkMapping(virtualLink, physicalLink);
	return true;
    }
    
    /**
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     * @return success
     */
    public boolean addPhysicalSwitchMapping(PhysicalSwitch physicalSwitch, OVXSwitch virtualSwitch) {
	this.physicalSwitchMap.get(physicalSwitch).put(virtualSwitch.getTenantId(), virtualSwitch);
	return true;
    }
    
    /**
     * sets up the mapping from the physical link to the virtualLinks which
     * contain the given physical link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     * @return success
     */
    public boolean addPhysicalLinkMapping(PhysicalLink physicalLink, OVXLink virtualLink) {
	this.physicalLinkMap.get(physicalLink).put(virtualLink.getTenantId(), virtualLink);
	return true;
    }
 
    /**
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     * @param physicalSwitch
     * 
     * @return success
     */
    public boolean addVirtualSwitchMapping(OVXSwitch virtualSwitch, PhysicalSwitch physicalSwitch) {
	this.virtualSwitchMap.get(virtualSwitch).add(physicalSwitch);
	return true;
    }

    /**
     * maps the virtual link to the physical links that it contains
     * 
     * @param virtualLink
     * @param physicalLinks
     * @return success
     */
    public boolean addVirtualLinkMapping(OVXLink virtualLink, PhysicalLink physicalLink) {
	this.virtualLinkMap.get(virtualLink).add(physicalLink);
	return true;
    }
    
    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     * 
     * @return success
     */
    public boolean addNetworkMapping(OVXNetwork virtualNetwork) {
	this.networkMap.put(virtualNetwork.getTenantId(), virtualNetwork);
	return true;
    }
    
    //Access objects from dictionary given the key
    
    /**
     * get the virtualSwitch which has been specified by the physicalSwitch
     * and tenantId
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    public OVXSwitch getVirtualSwitch(PhysicalSwitch physicalSwitch, Integer tenantId) {

	return this.physicalSwitchMap.get(physicalSwitch).get(tenantId);
    }
    
    /**
     * get the virtualLink which has been specified by the physicalLink and
     * the tenantId. This function will return a list of virtualLinks all of
     * which contain the specified physicalLink in the tenantId.
     * 
     * @param physicalLink
     * 
     * @return virtualLink
     */
    public OVXLink getVirtualLink(PhysicalLink physicalLink, Integer tenantId) {

	return this.physicalLinkMap.get(physicalLink).get(tenantId);
    }

    /**
     * get the physicalLinks that all make up a specified virtualLink.
     * Return a list of all the physicalLinks that make up the virtualLink
     * 
     * @param virtualLink
     * 
     * @return physicalLinks
     */
    public ArrayList<PhysicalLink> getPhysicalLinks(OVXLink virtualLink) {

	return this.virtualLinkMap.get(virtualLink);
    }
    
    /**
     * get the physicalSwitches that are contained in the virtualSwitch. for
     * a bigswitch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    public ArrayList<PhysicalSwitch> getPhysicalSwitches(OVXSwitch virtualSwitch) {

	return this.virtualSwitchMap.get(virtualSwitch);
    }
    
    /**
     * using the tenantId return the OVXNetwork object which is reffered to by
     * the specified tenantId.
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    public OVXNetwork getVirtualNetwork(Integer tenantId) {
	
	return this.networkMap.get(tenantId);
    }
    
    // Remove objects from dictionary
}
