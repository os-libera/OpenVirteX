package net.onrc.openvirtex.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.packet.LLDP;
import net.onrc.openvirtex.packet.LLDPTLV;

/**
 * LLDP packets OpenVirteX uses for discovery of physical network topology.
 * Refer to IEEE Std 802.1ABTM-2009 for more information.
 *
 */
@SuppressWarnings("rawtypes")
public class OVXLLDP extends LLDP {

	// ON.Lab OUI and OVX name for organizationally specific TLVs  
	public static final byte ONLAB_OUI[] = { (byte) 0xa4, 0x23, 0x05 };
	public static final String OVX_NAME = "OpenVirteX";
	public static final byte[] LLDP_NICIRA = { 0x01, 0x23, 0x20, 0x00, 0x00, 0x01 };
	public static final byte[] LLDP_MULTICAST = { 0x01, (byte) 0x80, (byte) 0xc2, 0x00, 0x00, 0x0e };
	public static final byte[] BDDP_MULTICAST = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,(byte) 0xff };
	public static final short ETHERTYPE_VLAN = (short) 0x8100;

	// TLV constants: type, size and subtype
	// Organizationally specific TLV also have packet offset and contents of TLV header  
	private static byte CHASSIS_TLV_TYPE = 1;
	private static byte CHASSIS_TLV_SIZE = 7;
	private static byte CHASSIS_TLV_SUBTYPE = 4;

	private static byte PORT_TLV_TYPE = 2;
	private static byte PORT_TLV_SIZE = 3;
	private static byte PORT_TLV_SUBTYPE = 2;

	private static byte TTL_TLV_TYPE = 3;
	private static byte TTL_TLV_SIZE = 2;

	private static byte NAME_TLV_TYPE = 127;
	private static byte NAME_TLV_SIZE = (byte) (4 + OVXLLDP.OVX_NAME.length());		// 4 = OUI (3) + subtype (1)
	private static byte NAME_TLV_SUBTYPE = 1;
	private static short NAME_TLV_OFFSET = 32;
	private static short NAME_TLV_HEADER = (short) ((NAME_TLV_TYPE << 9) | NAME_TLV_SIZE);
	// Contents of full name TLV
	private static byte[] NAME_TLV = ByteBuffer.allocate(NAME_TLV_SIZE + 2).
			putShort(NAME_TLV_HEADER).put(ONLAB_OUI).put(NAME_TLV_SUBTYPE).put(OVX_NAME.getBytes()).array();

	private static byte DPID_TLV_TYPE = 127;
	private static byte DPID_TLV_SIZE = (byte) (12);		// 12 = OUI (3) + subtype (1) + dpid (8)  
	private static byte DPID_TLV_SUBTYPE = 2;
	private static short DPID_TLV_HEADER = (short) ((DPID_TLV_TYPE << 9) | DPID_TLV_SIZE);
	// Contents of dpid TLV
	// Note that this does *not* contain the actual dpid since we cannot match on it
	private static byte[] DPID_TLV = ByteBuffer.allocate(DPID_TLV_SIZE + 2 - 8).
			putShort(DPID_TLV_HEADER).put(ONLAB_OUI).put(DPID_TLV_SUBTYPE).array();

	// Pre-built contents of both organizationally specific TLVs
	private static byte[] OUI_TLV = ArrayUtils.addAll(NAME_TLV, DPID_TLV);
	
	// Default switch, port number and TTL
	private static byte[] DEFAULT_DPID = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	private static short DEFAULT_PORT = 0;
	private static short DEFAULT_TTL = 120;				// in seconds

	// Minimum and OVX-generated LLDP packet sizes
	private static short MINIMUM_LLDP_SIZE = 60;
	// Add 12 for 2-byte header of each TLV and a single EndOfLLDPTLV
	private static short OVX_LLDP_SIZE = (short) (CHASSIS_TLV_SIZE + PORT_TLV_SIZE + TTL_TLV_SIZE + NAME_TLV_SIZE + DPID_TLV_SIZE + 12);

	// Field offsets in OVX-generated LLDP
	private static short ETHERTYPE_OFFSET = 12;
	private static short PORT_OFFSET = 26;
	private static short DPID_OFFSET = 54;

	// Private member fields
	// Byte arrays for TLV information string
	private ByteBuffer bb;
	private byte[] chassisId = new byte[CHASSIS_TLV_SIZE];
	private byte[] portId = new byte[PORT_TLV_SIZE];
	private byte[] ttl = new byte[TTL_TLV_SIZE];
	private byte[] ouiName = new byte[NAME_TLV_SIZE];
	private byte[] ouiDpid = new byte[DPID_TLV_SIZE];

	// TLVs
	private LLDPTLV chassisTLV;
	private LLDPTLV portTLV;
	private LLDPTLV ttlTLV;
	private LLDPTLV ouiNameTLV;
	private LLDPTLV ouiDpidTLV;
	private List<LLDPTLV> optionalTLVList;

	private Switch sw = null;
	private Port port = null;

	public OVXLLDP() {
		// Create TLVs
		this.chassisTLV = new LLDPTLV();
		this.portTLV = new LLDPTLV();
		this.ttlTLV = new LLDPTLV();
		this.ouiNameTLV = new LLDPTLV();
		this.ouiDpidTLV = new LLDPTLV();
		this.optionalTLVList = new LinkedList<LLDPTLV>();
		this.optionalTLVList.add(this.ouiNameTLV);
		this.optionalTLVList.add(this.ouiDpidTLV);

		// Add TLVs to LLDP packet
		this.setChassisId(this.chassisTLV);
		this.setPortId(this.portTLV);
		this.setTtl(this.ttlTLV);
		this.setOptionalTLVList(this.optionalTLVList);

		// Set TLVs to default values
		this.setChassisTLV(DEFAULT_DPID);
		this.setPortTLV(DEFAULT_PORT);
		this.setTTLTLV(DEFAULT_TTL);
		this.setOUIName(OVXLLDP.OVX_NAME);
		this.setOUIDpid(DEFAULT_DPID);
	}

	/**
	 * Set chassis TLV
	 * Note that we can only put 6 bytes in the chassis ID,
	 * so we use another organizationally specific TLV to put
	 * the full dpid (see setOUIDpid()).
	 * @param dpid
	 */
	private void setChassisTLV(final byte[] dpid) {
		this.bb = ByteBuffer.wrap(this.chassisId);
		this.bb.put(CHASSIS_TLV_SUBTYPE);
		for (int i = 2; i < 8; i++)
			bb.put(dpid[i]);

		this.chassisTLV.setLength(CHASSIS_TLV_SIZE);
		this.chassisTLV.setType(CHASSIS_TLV_TYPE);
		this.chassisTLV.setValue(this.chassisId);
	}

	/**
	 * Set port TLV
	 * @param portNumber
	 */
	private void setPortTLV(final short portNumber) {
		this.bb = ByteBuffer.wrap(this.portId);
		this.bb.put(PORT_TLV_SUBTYPE);
		this.bb.putShort(portNumber);

		this.portTLV.setLength(PORT_TLV_SIZE);
		this.portTLV.setType(PORT_TLV_TYPE);
		this.portTLV.setValue(this.portId);
	}

	/**
	 * Set Time To Live TLV
	 * @param time
	 */
	private void setTTLTLV(final short time) {
		this.bb = ByteBuffer.wrap(this.ttl);
		this.bb.putShort(time);

		this.ttlTLV.setLength(TTL_TLV_SIZE);
		this.ttlTLV.setType(TTL_TLV_TYPE);
		this.ttlTLV.setValue(this.ttl);
	}

	/**
	 * Set organizationally specific TLV for OVX name (subtype 1)
	 * @param name
	 */
	private void setOUIName(final String name) {
		this.bb = ByteBuffer.wrap(ouiName);
		this.bb.put(OVXLLDP.ONLAB_OUI);
		this.bb.put(NAME_TLV_SUBTYPE);
		this.bb.put(name.getBytes());

		this.ouiNameTLV.setLength(NAME_TLV_SIZE);
		this.ouiNameTLV.setType(NAME_TLV_TYPE);
		this.ouiNameTLV.setValue(ouiName);
	}

	/**
	 * Set organizationally specific TLV for OVX full dpid (subtype 2)
	 * @param dpid
	 */
	private void setOUIDpid(final byte[] dpid) {
		this.bb = ByteBuffer.wrap(ouiDpid);
		this.bb.put(OVXLLDP.ONLAB_OUI);
		this.bb.put(DPID_TLV_SUBTYPE);
		this.bb.put(dpid);

		this.ouiDpidTLV.setLength(DPID_TLV_SIZE);
		this.ouiDpidTLV.setType(DPID_TLV_TYPE);
		this.ouiDpidTLV.setValue(ouiDpid);
	}

	/**
	 * Set switch dpid in LLDP packet.
	 * @param sw
	 */
	public void setSwitch(Switch sw) {
		this.sw = sw;
		final byte[] dpid = ByteBuffer.allocate(8).putLong(sw.getSwitchId()).array();
		this.setChassisTLV(dpid);
		this.setOUIDpid(dpid);
	}

	/**
	 * Set port in LLDP packet
	 * @param sw
	 */
	public void setPort(Port port) {
		this.port = port;
		short portNumber = this.port.getPortNumber();
		this.setPortTLV(portNumber);
	}

	/**
	 * Serializes full LLDP packet to byte array.
	 * Need to set both switch and port before you can serialize
	 */
	public byte[] serialize() {
		if (this.sw == null || this.port == null)
			return null;

		return super.serialize();
	}

	/**
	 * Checks if LLDP packet has correct size, LLDP multicast address, and ethertype.
	 * Packet assumed to have Ethernet header.
	 * 
	 * @param packet
	 * @return true if packet is LLDP, false otherwise
	 */
	public static boolean isLLDP(final byte[] packet) {
		// Does packet exist and does it have the mininum size?
		if (packet == null || packet.length < MINIMUM_LLDP_SIZE) {
			return false;
		}

		// Packet has LLDP multicast destination address?
		final ByteBuffer bb = ByteBuffer.wrap(packet);
		final byte[] dst = new byte[6];
		bb.get(dst);
		
		if (!(Arrays.equals(dst, OVXLLDP.LLDP_NICIRA) 
				|| Arrays.equals(dst, OVXLLDP.LLDP_MULTICAST) 
				|| Arrays.equals(dst, OVXLLDP.BDDP_MULTICAST))) {
			
			
			return false;
		}

		// Fetch ethertype, skip VLAN tag if it's there
		short etherType = bb.getShort(ETHERTYPE_OFFSET);
		if (etherType == ETHERTYPE_VLAN)
			etherType = bb.getShort(ETHERTYPE_OFFSET + 4);
		
		// Check ethertype
		if (etherType == Ethernet.TYPE_LLDP)
			return true;
		if (etherType == Ethernet.TYPE_BSN)
			return true;
		
		
		return false;

	}

	/**
	 * Checks if packet has size of OVX-generated LLDP,
	 * and correctness of two organizationally specific TLVs that use ON.Lab's OUI.
	 * Assumes packet is valid LLDP packet
	 * 
	 * @param packet
	 * @return
	 */
	public static boolean isOVXLLDP(byte[] packet) {
		if (packet.length < OVX_LLDP_SIZE)
			return false;

		// Extra offset due to VLAN tag
		final ByteBuffer bb = ByteBuffer.wrap(packet);
		int offset = 0;
		if (bb.getShort(ETHERTYPE_OFFSET) == (short) 0x8100) {
			offset = 4;
			
		}

		// Compare packet's organizationally specific TLVs to the expected values
		for (int i = 0; i < OUI_TLV.length; i++) {
			if (packet[NAME_TLV_OFFSET + offset + i] != OUI_TLV[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Extract dpid and port from OVX-generated LLDP packet
	 * 
	 * @param packet
	 * @return Dpid and port
	 */
	static public DPIDandPort parseLLDP(final byte[] packet) {
		final ByteBuffer bb = ByteBuffer.wrap(packet);

		// Extra offset due to VLAN tag
		int offset = 0;
		if (bb.getShort(ETHERTYPE_OFFSET) == (short) 0x8100) 
			offset = 4;
		
		final short port = bb.getShort(PORT_OFFSET + offset);
		final long dpid = bb.getLong(DPID_OFFSET + offset);
		
		return new DPIDandPort(dpid, port);
	}	
}
