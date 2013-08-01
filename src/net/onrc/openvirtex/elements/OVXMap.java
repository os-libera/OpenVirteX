/**
 * Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER
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

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

public class OVXMap implements Mappable {

    private static OVXMap                                                    mapInstance = new OVXMap();

    ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>                  virtualSwitchMap;
    ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>> physicalSwitchMap;
    ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>                      virtualLinkMap;
    ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>>     physicalLinkMap;
    ConcurrentHashMap<Integer, OVXNetwork>                                   networkMap;
    RadixTree<OVXIPAddress>                                                        physicalIPMap;
    RadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>> virtualIPMap;
    /**
     * constructor for OVXMap will be an empty constructor
     */
    private OVXMap() {
	this.virtualSwitchMap = new ConcurrentHashMap<OVXSwitch, ArrayList<PhysicalSwitch>>();
	this.physicalSwitchMap = new ConcurrentHashMap<PhysicalSwitch, ConcurrentHashMap<Integer, OVXSwitch>>();
	this.virtualLinkMap = new ConcurrentHashMap<OVXLink, ArrayList<PhysicalLink>>();
	this.physicalLinkMap = new ConcurrentHashMap<PhysicalLink, ConcurrentHashMap<Integer, OVXLink>>();
	this.networkMap = new ConcurrentHashMap<Integer, OVXNetwork>();
	this.physicalIPMap = new ConcurrentRadixTree<OVXIPAddress>(
	        new DefaultCharArrayNodeFactory());
	this.virtualIPMap = new ConcurrentRadixTree<ConcurrentHashMap<Integer, PhysicalIPAddress>>(new DefaultCharArrayNodeFactory());
    }

    /**
     * getInstance will get the instance of the class and if this already exists
     * then the existing object will be returned. This is a singleton class.
     * 
     * @return OVXMap
     */
    public static OVXMap getInstance() {
	if (OVXMap.mapInstance == null) {
	    OVXMap.mapInstance = new OVXMap();
	}
	return OVXMap.mapInstance;
    }

    // ADD objects to dictionary

    /**
     * create the mapping between virtual switch to physical switch and from
     * physical switch to virtual switch
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     */
    @Override
    public void addSwitchMapping(final PhysicalSwitch physicalSwitch,
	    final OVXSwitch virtualSwitch) {
	this.addPhysicalSwitchMapping(physicalSwitch, virtualSwitch);
	this.addVirtualSwitchMapping(virtualSwitch, physicalSwitch);
    }

    /**
     * create the mapping between the virtual link to phsyical link and physical
     * link to virtual link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     */
    @Override
    public void addLinkMapping(final PhysicalLink physicalLink,
	    final OVXLink virtualLink) {
	this.addPhysicalLinkMapping(physicalLink, virtualLink);
	this.addVirtualLinkMapping(virtualLink, physicalLink);
    }

    /**
     * This is the generic function which takes as arguments the PhysicalIPAddress
     * and the OVXIPAddress. This will add the value into both the physical
     * to virtual map and in the other direction.
     * 
     * @param physicalIP
     * @param virtualIP
     */
    public void addIPMapping(PhysicalIPAddress physicalIP, OVXIPAddress virtualIP) {
	this.addPhysicalIPMapping(physicalIP, virtualIP);
	this.addOVXIPMapping(virtualIP, physicalIP);
    }
    
    /**
     * This function will create a map indexed on the key PhysicalIPAddress with
     * value OVXIPAddress.
     *  
     * @param physicalIP
     * @param virtualIP
     */
    public void addPhysicalIPMapping(PhysicalIPAddress physicalIP, OVXIPAddress virtualIP) {
	this.physicalIPMap.put(physicalIP.toString(), virtualIP);
    }
    
    /**
     * This function will create a map indexed on the key OVXIPAddress with value
     * PhysicalIPAddress
     * 
     * @param virtualIP
     * @param physicalIP
     */
    public void addOVXIPMapping(OVXIPAddress virtualIP, PhysicalIPAddress physicalIP) {
	ConcurrentHashMap<Integer, PhysicalIPAddress> ipMap = this.virtualIPMap.getValueForExactKey(virtualIP.toString());
	if (ipMap == null)
	    ipMap = new ConcurrentHashMap<Integer, PhysicalIPAddress>();
	ipMap.put(virtualIP.getTenantId(),physicalIP);
    }
    
    /**
     * sets up the mapping from the physicalSwitch to the virtualSwitch
     * which has been specified
     * 
     * @param physicalSwitch
     * @param virtualSwitch
     * 
     */
    public void addPhysicalSwitchMapping(final PhysicalSwitch physicalSwitch,
	    final OVXSwitch virtualSwitch) {
	this.physicalSwitchMap.get(physicalSwitch).put(
	        virtualSwitch.getTenantId(), virtualSwitch);
    }

    /**
     * sets up the mapping from the physical link to the virtualLinks which
     * contain the given physical link
     * 
     * @param physicalLink
     * @param virtualLink
     * 
     */
    public void addPhysicalLinkMapping(final PhysicalLink physicalLink,
	    final OVXLink virtualLink) {
	this.physicalLinkMap.get(physicalLink).put(virtualLink.getTenantId(),
	        virtualLink);
    }

    /**
     * sets up the mapping from the virtualSwitch to the physicalSwitch
     * which has been specified
     * 
     * @param virtualSwitch
     * @param physicalSwitch
     * 
     */
    public void addVirtualSwitchMapping(final OVXSwitch virtualSwitch,
	    final PhysicalSwitch physicalSwitch) {
	this.virtualSwitchMap.get(virtualSwitch).add(physicalSwitch);
    }

    /**
     * maps the virtual link to the physical links that it contains
     * 
     * @param virtualLink
     * @param physicalLinks
     */
    public void addVirtualLinkMapping(final OVXLink virtualLink,
	    final PhysicalLink physicalLink) {
	this.virtualLinkMap.get(virtualLink).add(physicalLink);
    }

    /**
     * Maintain a list of all the virtualNetworks in the system
     * indexed by the tenant id mapping to VirtualNetworks
     * 
     * @param virtualNetwork
     * 
     */
    @Override
    public void addNetworkMapping(final OVXNetwork virtualNetwork) {
	this.networkMap.put(virtualNetwork.getTenantId(), virtualNetwork);
    }

    // Access objects from dictionary given the key

    /**
     * get the virtualSwitch which has been specified by the physicalSwitch
     * and tenantId
     * 
     * @param physicalSwitch
     * 
     * @return virtualSwitches
     */
    @Override
    public OVXSwitch getVirtualSwitch(final PhysicalSwitch physicalSwitch,
	    final Integer tenantId) {
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
    @Override
    public OVXLink getVirtualLink(final PhysicalLink physicalLink,
	    final Integer tenantId) {
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
    @Override
    public List<PhysicalLink> getPhysicalLinks(final OVXLink virtualLink) {

	return this.virtualLinkMap.get(virtualLink);
    }

    /**
     * get the physicalSwitches that are contained in the virtualSwitch. for
     * a big switch this will be multiple physicalSwitches
     * 
     * @param virtualSwitch
     * 
     * @return physicalSwitches
     */
    @Override
    public List<PhysicalSwitch> getPhysicalSwitches(
	    final OVXSwitch virtualSwitch) {
	return this.virtualSwitchMap.get(virtualSwitch);
    }

    /**
     * use the tenantId to return the OVXNetwork object
     * 
     * @param tenantId
     * 
     * @return virtualNetwork
     */
    @Override
    public OVXNetwork getVirtualNetwork(final Integer tenantId) {

	return this.networkMap.get(tenantId);
    }

    // Remove objects from dictionary
}
