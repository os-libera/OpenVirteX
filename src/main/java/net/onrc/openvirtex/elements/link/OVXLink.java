/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/


package net.onrc.openvirtex.elements.link;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * The Class OVXLink.
 * 
 */
public class OVXLink extends Link<OVXPort, OVXSwitch> {
	Logger	log = LogManager.getLogger(OVXLink.class.getName());
	
	/** The link id. */
	
	@SerializedName("linkId")
	@Expose
	private final Integer linkId;

	/** The tenant id. */
	@SerializedName("tenantId")
	@Expose
	private final Integer tenantId;

	/**
	 * Instantiates a new virtual link.
	 * 
	 * @param linkId
	 *            link id
	 * @param tenantId
	 *            tenant id
	 * @param srcPort
	 *            virtual source port
	 * @param dstPort
	 *            virtual destination port
	 */
	public OVXLink(final Integer linkId, final Integer tenantId,
			final OVXPort srcPort, final OVXPort dstPort) {
		super(srcPort, dstPort);
		this.linkId = linkId;
		this.tenantId = tenantId;
		srcPort.setOutLink(this);
		dstPort.setInLink(this);
	}

	/**
	 * Gets the link id.
	 * 
	 * @return the link id
	 */
	public Integer getLinkId() {
		return this.linkId;
	}

	/**
	 * Gets the tenant id.
	 * 
	 * @return the tenant id
	 */
	public Integer getTenantId() {
		return this.tenantId;
	}

	/**
	 * Register mapping between virtual link and physical path
	 * 
	 * @param physicalLinks
	 */
	public void register(final List<PhysicalLink> physicalLinks) {
		this.srcPort.getParentSwitch().getMap().addLinks(physicalLinks, this);
	}

	@Override
	public void unregister() {
	    Mappable map = this.srcPort.getParentSwitch().getMap();
	    map.removeVirtualLink(this);
	    map.getVirtualNetwork(tenantId).removeLink(this);
	    this.srcPort.unregister();
	}
	
	public void generateLinkFMs(OFFlowMod fm, Integer flowId, OVXSwitch sw) {
    	Short inPort = 0;
    	Short outPort = 0;
    	
    	OFFlowMod linkFM = null;
		try {
			linkFM = fm.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			return;
		}
		if (fm.getMatch().getDataLayerType() != Ethernet.TYPE_ARP) 
			rewriteMatch(linkFM, sw);
		linkFM.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		//TODO: check if works also with the original match
		OFMatch match = linkFM.getMatch().clone();
		
    	OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();
    	OVXLinkUtils lUtils = new OVXLinkUtils(this.tenantId, this.linkId, flowId);
		//TODO: Need to check that the values in linkId and flowId don't exceed their space
		if (linkField == OVXLinkField.MAC_ADDRESS) {
			match.setDataLayerSource(lUtils.getSrcMac().toBytes());
			match.setDataLayerDestination(lUtils.getDstMac().toBytes());
    	}
    	else if (linkField == OVXLinkField.VLAN)
    		match.setDataLayerVirtualLan((short) lUtils.getVlan());
    	
		LinkedList<OFFlowMod> fmList = new LinkedList<>();
		LinkedList<PhysicalSwitch> sws = new  LinkedList<PhysicalSwitch>();
    	for (PhysicalLink phyLink : sw.getMap().getPhysicalLinks(this)) {
    		if (inPort != 0) {
    			outPort = phyLink.getSrcPort().getPortNumber();
    			match.setInputPort(inPort);
    			linkFM.setMatch(match);
    			linkFM.setOutPort(outPort);
    			linkFM.setLengthU(OVXFlowMod.MINIMUM_LENGTH + OVXActionOutput.MINIMUM_LENGTH);
    			linkFM.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort, (short) 0xffff)));
    			try {
					fmList.add(0, linkFM.clone());
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			sws.add(0, phyLink.getSrcPort().getParentSwitch());
    			log.debug("Sending flowMod [link-intermediate] to {}: {}", phyLink.getSrcPort().getParentSwitch().getName(), linkFM);
    		}
    		inPort = phyLink.getDstPort().getPortNumber();
    	}
    	for (int i = 0 ; i < sws.size() ; i++)
    		sws.get(i).sendMsg(fmList.get(i), sws.get(i));
    	
    	try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
    }
    
    private void rewriteMatch(OFFlowMod fm, OVXSwitch sw) {
    	final Mappable map = sw.getMap();

    	// TODO: handle IP ranges
    	if (!fm.getMatch().getWildcardObj().isWildcarded(Flag.NW_SRC)) {
    		final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
    				fm.getMatch().getNetworkSource());
    		PhysicalIPAddress pip = map.getPhysicalIP(vip,
    				sw.getTenantId());
    		if (pip == null) {
    			pip = new PhysicalIPAddress(map.getVirtualNetwork(
    					sw.getTenantId()).nextIP());
    			this.log.debug(
    					"Adding IP mapping {} -> {} for tenant {} at switch {}",
    					vip, pip, sw.getTenantId(), sw.getName());
    			map.addIP(pip, vip);
    		}
    		fm.getMatch().setNetworkSource(pip.getIp());
    	}

    	if (!fm.getMatch().getWildcardObj().isWildcarded(Flag.NW_DST)) {
    		final OVXIPAddress vip = new OVXIPAddress(sw.getTenantId(),
    				fm.getMatch().getNetworkDestination());
    		PhysicalIPAddress pip = map.getPhysicalIP(vip,
    				sw.getTenantId());
    		if (pip == null) {
    			pip = new PhysicalIPAddress(map.getVirtualNetwork(
    					sw.getTenantId()).nextIP());
    			this.log.debug(
    					"Adding IP mapping {} -> {} for tenant {} at switch {}",
    					vip, pip, sw.getTenantId(), sw.getName());
    			map.addIP(pip, vip);
    		}
    		fm.getMatch().setNetworkDestination(pip.getIp());
    	}

    }
    
	public List<OFAction> setLinkFields(Integer tenantId, Integer linkId, Integer flowId) {
		
		List<OFAction> actions = new LinkedList<OFAction>();
		OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();
		OVXLinkUtils lUtils = new OVXLinkUtils(tenantId, linkId, flowId);
		//TODO: Need to check that the values in linkId and flowId don't exceed their space
		if (linkField == OVXLinkField.MAC_ADDRESS) {
			actions.add(new OFActionDataLayerSource(lUtils.getSrcMac().toBytes()));
			actions.add(new OFActionDataLayerDestination(lUtils.getDstMac().toBytes()));
    	}
    	else if (linkField == OVXLinkField.VLAN)
    		actions.add(new OFActionVirtualLanIdentifier(lUtils.getVlan()));
		return actions;
	}
	
	public List<OFAction> unsetLinkFields(OVXMatch match, OVXSwitch sw) throws DroppedMessageException {
		Integer flowId = 0;
		List<OFAction> actions = new LinkedList<OFAction>();
		
		OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();
		//TODO: Need to check that the values in linkId and flowId don't exceed their space
		if (linkField == OVXLinkField.MAC_ADDRESS) {
			flowId = sw.getMap().getVirtualNetwork(this.tenantId).
					getFlowId(match.getDataLayerSource(), match.getDataLayerDestination());
			if (flowId != null) {
    			OVXLinkUtils lUtils = new OVXLinkUtils(this.getTenantId(), linkId, flowId);
    			LinkedList<MACAddress> macList = sw.getMap().getVirtualNetwork(this.tenantId).getFlowValues(lUtils.getFlowId());
    			if (macList.size() == 0)
    				throw new DroppedMessageException();
    			actions.add(new OFActionDataLayerSource(macList.get(0).toBytes()));
    			actions.add(new OFActionDataLayerDestination(macList.get(1).toBytes()));
			}
			else {
				PhysicalPort port = sw.getPort(match.getInputPort()).getPhysicalPort();
				log.error("not found the flowId for packet coming from switch {}, port {} with this match {}", port.getParentSwitch(), 
						port.getPortNumber(), match );
			}
    	}
    	else if (linkField == OVXLinkField.VLAN) {
    		//TODO: retrieve flowId from VLAN value
    		//ovxMatch.setDataLayerVirtualLan((short) (linkId.shortValue()<<6 + flowId));
    	}
		return actions;
	}
	
	public OFMatch rewriteMatch(OFMatch match, OVXSwitch sw) {
		OVXLinkField linkField = OpenVirteXController.getInstance().getOvxLinkField();
		//TODO: Need to check that the values in linkId and flowId don't exceed their space
		if (linkField == OVXLinkField.MAC_ADDRESS) {
			Integer flowId = sw.getMap().getVirtualNetwork(this.tenantId).
					getFlowId(match.getDataLayerSource(), match.getDataLayerDestination());
			if (flowId != null) {
    			OVXLinkUtils lUtils = new OVXLinkUtils(sw.getTenantId(), linkId, flowId);
    			match.setDataLayerSource(lUtils.getSrcMac().toBytes());
    			match.setDataLayerDestination(lUtils.getDstMac().toBytes());
			}
    	}
    	else if (linkField == OVXLinkField.VLAN) {
    		//TODO: retrieve flowId from VLAN value
    		//ovxMatch.setDataLayerVirtualLan((short) (linkId.shortValue()<<6 + flowId));
    	}
		return match;
	}
	
}
