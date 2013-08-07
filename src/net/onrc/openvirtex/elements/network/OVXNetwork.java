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

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXPacketIn;
import net.onrc.openvirtex.messages.OVXPacketOut;
import net.onrc.openvirtex.messages.lldp.LLDPUtil;
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
	super();
	this.tenantId = tenantId;
	this.ctrlProto = ctrlProto;
	this.ctrlAddr = ctrlAddr;
	this.ctrlPort = ctrlPort;
	this.network = network;
	this.mask = mask;
    }

    public void register() {
	OVXMap.getInstance().addNetwork(this);
    }

    public Integer getTenantId() {
	return this.tenantId;
    }

    public void setTenantId(final Integer tenantId) {
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

    public OVXSwitch addSwitch(final OVXSwitch sw) {
	// switch = new OVXBigSwitch();
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
    // sw argument is irrelevant in this case
    public void handleLLDP(final OFMessage msg, final Switch sw) {
	final OVXPacketOut po = (OVXPacketOut) msg;
	final byte[] pkt = po.getPacketData();
	if (LLDPUtil.checkLLDP(pkt)) {
	    final DPIDandPort dp = LLDPUtil.parseLLDP(pkt);
	    // TODO: check if dpid present
	    final OVXSwitch lldpSwitch = this.dpidMap.get(dp.getDpid());
	    final OVXPort port = lldpSwitch.getPort(dp.getPort());
	    final OVXPort neighbour = this.neighbourPortMap.get(port);
	    // Return other end
	    OVXPacketIn pi = new OVXPacketIn();
	    pi.setInPort(neighbour.getPortNumber());
	    pi.setPacketData(pkt);
	    neighbour.getParentSwitch().sendMsg(pi, this);
	} else {
	    System.out.println("not a valid LLDP");
	}
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
	// Do nothing

    }

    @Override
    public String getName() {
	return "Virtual network:" + this.tenantId.toString();
    }
}
