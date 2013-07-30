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

package net.onrc.openvirtex.elements.network;

import java.util.HashMap;
import java.util.HashSet;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFMessage;

public class OVXNetwork extends Network<OVXSwitch, OVXPort, OVXLink> {

    // public VLinkManager vLinkMgmt;

    private Integer                        tenantId;
    private final String                   ctrlProto;
    private final IPAddress                ctrlAddr;
    private final short                    ctrlPort;
    private IPAddress                      network;
    private short                          mask;
    private HashMap<IPAddress, MACAddress> gwsMap;
    private boolean                        bootState;

    public OVXNetwork(final Integer tenantId, final String ctrlProto,
	    final IPAddress ctrlAddr, final short ctrlPort,
	    final IPAddress network, final short mask) {
	this.tenantId = tenantId;
	this.ctrlProto = ctrlProto;
	this.ctrlAddr = ctrlAddr;
	this.ctrlPort = ctrlPort;
	this.network = network;
	this.mask = mask;
    }

    public boolean register() {
	// TODO = access map
	return true;
    }

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

    public OVXSwitch addSwitch(final HashSet<OVXSwitch> swSet) {
	return new OVXBigSwitch();
    }

    public OVXPort addHost(final PhysicalPort port) {
	return new OVXPort();
    }

    public OVXLink addLink(final PhysicalPort in_port,
	    final PhysicalPort out_port) {
	return new OVXLink();
    }

    public void addGateway(final IPAddress ip) {

    }

    public boolean boot() {
	return true;
    }

    @Override
    public void handleIO(final OFMessage msgs) {
	// TODO Auto-generated method stub
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	// TODO Auto-generated method stub
    }
}
