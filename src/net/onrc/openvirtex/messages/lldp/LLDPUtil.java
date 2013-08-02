/**
 *
 */
package net.onrc.openvirtex.messages.lldp;

import java.nio.ByteBuffer;

import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.elements.datapath.Switch;

import org.jboss.netty.buffer.ChannelBuffers;
import org.openflow.util.StringByteSerializer;

/**
 * Set of utilities for handling our LLDP virtualisation hacks
 * 
 * @author capveg
 * 
 */
public class LLDPUtil {
    final private static int   LLDPLen          = 128;
    final static byte          lldpSysD[]       = { 0x0c, 0x08 }; // Type 6,
	                                                          // length 8
    final public static short  ETHER_LLDP       = (short) 0x88cc;
    final public static short  ETHER_VLAN       = (short) 0x8100;
    final public static byte[] LLDP_MULTICAST   = { 0x01, 0x23, 0x20, 0x00,
	    0x00, 0x01                         };
    final public static int    MIN_FV_NAME      = 20;
    final public static byte   OUI_TYPE         = 127;
    public final static int    FLOWNAMELEN_LEN  = 1;
    public final static int    FLOWNAMELEN_NULL = 1;

    /**
     * Is this an lldp packet?
     * 
     * @param po
     * @return
     */

    static public boolean LLDPCheck(final byte[] packetArray) {
	if (packetArray == null || packetArray.length < 14) {
	    return false; // not lddp if no packet exists or too short
	}
	final ByteBuffer packet = ByteBuffer.wrap(packetArray);
	short ether_type = packet.getShort(12);
	// FVLog.log(LogLevel.DEBUG,null,"Checking if the pkt is LLDP?",
	// ether_type );
	if (ether_type == LLDPUtil.ETHER_VLAN) {
	    ether_type = packet.getShort(16);
	}
	if (ether_type != LLDPUtil.ETHER_LLDP) {
	    // FVLog.log(LogLevel.DEBUG,null,"The pkt is not LLDP" );
	    return false;
	}
	// TODO think about checking for NOX OID
	return true;
    }

    public byte[] makeLLDP(final Port port) {
	final short portNumber = port.getPortNumber();
	final byte[] hardwareAddress = port.getHardwareAddress().toBytes();
	final long dpid = ((Switch) port.getParentSwitch()).getSwitchId();
	// TODO: put this somewhere where it makes sense
	final String ovxName = "OpenVirteX";

	final int size = LLDPUtil.LLDPLen; // needs to be some minsize to avoid
	                                   // ethernet problems
	final byte[] buf = new byte[size];
	final ByteBuffer bb = ByteBuffer.wrap(buf);

	// LLDP Framing
	bb.put(LLDPUtil.LLDP_MULTICAST); // dst addr
	bb.put(hardwareAddress); // src addr
	bb.putShort(LLDPUtil.ETHER_LLDP);

	// TLV type is 7 bits, length is 9
	// I precomputed them on byte boundaries to save time and sanity

	// NOX only supports Chassis ID subtype 1 and Port ID subtype 2
	// which we can't jam a full datapath ID into.
	// So we have to supply those here, and then overload the
	// System Decription TLV to dump in the datapath ID

	// Chassis ID TLV
	final byte chassis[] = { 0x02, 0x07, // type 1, length 7 (1 + 6)
	        0x04 }; // subtype = subtype MAC address
	bb.put(chassis);
	bb.put(hardwareAddress);

	// Port ID TLV
	final byte id[] = { 0x04, 0x03, // type 2, length 3
	        0x02 }; // Subtype Port
	bb.put(id);
	bb.putShort(portNumber);

	// TTL TLV
	final byte ttl[] = { 0x06, 0x02, 0x00, 0x78 };
	bb.put(ttl); // type 3, length 2, 120 seconds

	// SysD TLV
	bb.put(LLDPUtil.lldpSysD);
	bb.putLong(dpid);

	// OUI TLV
	final int ouiLen = 4 + ovxName.length() + LLDPUtil.FLOWNAMELEN_LEN
	        + LLDPUtil.FLOWNAMELEN_NULL;
	// 4 - length of OUI Id + it's subtype
	final int ouiHeader = ouiLen & 0x1ff
	        | (LLDPUtil.OUI_TYPE & 0x007f) << 9;
	bb.putShort((short) ouiHeader);

	// ON.Lab OUI = a42305 and assigning the subtype to 0x01
	final byte oui[] = { (byte) 0xa4, (byte) 0x23, (byte) 0x05 };
	// byte oui[] = {0x0a, 0x04, 0x02, 0x03, 0x00, 0x05};
	bb.put(oui);
	final byte ouiSubtype[] = { 0x01 };
	bb.put(ouiSubtype);
	StringByteSerializer.writeTo(ChannelBuffers.wrappedBuffer(bb),
	        ovxName.length() + 1, ovxName);
	bb.put((byte) (ovxName.length() + 1));

	// EndOfLLDPDU TLV
	final byte endType[] = { 0x00 };
	bb.put(endType);
	final byte endLength[] = { 0x00 };
	bb.put(endLength);

	while (bb.position() <= size - 4) {
	    bb.putInt(0xcafebabe); // fill with well known padding
	}
	return buf;
    }

}
