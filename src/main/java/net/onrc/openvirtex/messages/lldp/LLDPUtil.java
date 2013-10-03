/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
/**
 *
 */
package net.onrc.openvirtex.messages.lldp;

import java.nio.ByteBuffer;
import java.util.Arrays;

import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;

/**
 * Set of utilities for handling LLDP packets Heavily based on FlowVisor 1.2
 * implemenation.
 * 
 */
public class LLDPUtil {
	final private static int LLDPLen = 128;
	final static byte lldpSysD[] = { 0x0c, 0x08 }; // Type 6,
	// length 8
	final public static short ETHER_LLDP = (short) 0x88cc;
	final public static short ETHER_VLAN = (short) 0x8100;
	final public static byte[] LLDP_MULTICAST = { 0x01, 0x23, 0x20, 0x00, 0x00,
			0x01 };
	final public static int MIN_FV_NAME = 20;
	final public static byte OUI_TYPE = 127;
	public final static int FLOWNAMELEN_LEN = 1;
	public final static int FLOWNAMELEN_NULL = 1;

	/**
	 * Is this an lldp packet?
	 * 
	 * @param po
	 * @return
	 */

	public static boolean checkLLDP(final byte[] packetArray) {
		if (packetArray == null || packetArray.length < 14) {
			return false; // not lddp if no packet exists or too short
		}
		final ByteBuffer packet = ByteBuffer.wrap(packetArray);
		short ether_type = packet.getShort(12);
		if (ether_type == LLDPUtil.ETHER_VLAN) {
			ether_type = packet.getShort(16);
		}
		if (ether_type != LLDPUtil.ETHER_LLDP) {
			return false;
		}
		return true;
	}

	/**
	 * Create an LLDP packet for the specified output port
	 * 
	 * @param port
	 * @return The LLDP packet
	 */
	public static byte[] makeLLDP(final Port port) {
		final short portNumber = port.getPortNumber();
		final byte[] hardwareAddress = port.getHardwareAddress();
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
		bb.putLong(((Switch) port.getParentSwitch()).getSwitchId());

		// OUI TLV
		// final int ouiLen = 4 + ovxName.length() + LLDPUtil.FLOWNAMELEN_LEN
		// + LLDPUtil.FLOWNAMELEN_NULL;
		final int ouiLen = 4 + ovxName.length() + LLDPUtil.FLOWNAMELEN_LEN;
		// 4 - length of OUI Id + it's subtype
		final int ouiHeader = ouiLen & 0x1ff
				| (LLDPUtil.OUI_TYPE & 0x007f) << 9;
		bb.putShort((short) ouiHeader);

		// ON.Lab OUI = a42305 and assigning the subtype to 0x01
		final byte oui[] = { (byte) 0xa4, (byte) 0x23, (byte) 0x05 };
		bb.put(oui);
		final byte ouiSubtype[] = { 0x01 };
		bb.put(ouiSubtype);
		// TODO: what does this do and why does it fail?
		// StringByteSerializer.writeTo(ChannelBuffers.copiedBuffer(bb),
		// ovxName.length() + 1, ovxName);
		// TODO: this is probably not the most portable code
		bb.put(ovxName.getBytes());
		// bb.put((byte) 0);
		bb.put((byte) ovxName.length());

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

	/**
	 * Extract dpid and port from LLDP packet TODO: generalize this so we can
	 * parse OVX-generated LLDPs and generic controller-generated ones
	 * 
	 * @param packet
	 * @return Dpid and port
	 */
	static public DPIDandPort parseLLDP(final byte[] packet) {
		// TODO: generalize this so we can parse OVX-generated LLDPs
		// and generic controller-generated ones
		// LLDP packets sent by FV should have the following byte offsets:
		// 0 - dst addr (MAC)
		// 6 - src addr (MAC)
		// 12 - ether lldp
		// 14 - chassis id tl
		// 16 - subtype
		// 17 - src addr (MAC)
		// 23 - port id tl
		// 25 - subtype
		// 26 - port num
		// 28 - ttl tl
		// 30 - ttl value
		// 32 - sysdesc tl
		// 34 - dpid
		// 42 - oui header
		// 44 - oui id
		// 47 - oui subtype
		// 48 - oui string (2 bytes for fvName; 1 for null; 1 for fvNameLength)
		// 52 - endOfLLDPDU
		// 54 - padding

		int vlan_offset = 0;
		final ByteBuffer bb = ByteBuffer.wrap(packet);
		final byte[] dst = new byte[6];
		bb.get(dst);
		// could move this to LLDPCheck
		if (!Arrays.equals(dst, LLDPUtil.LLDP_MULTICAST)) {
			return null;
		}
		bb.position(12);
		short etherType = bb.getShort();
		while (etherType == LLDPUtil.ETHER_VLAN) {
			vlan_offset += 4;
			etherType = bb.getShort(); // noop to advance two bytes
			etherType = bb.getShort();
		}
		if (etherType != LLDPUtil.ETHER_LLDP) {
			return null;
		}
		bb.position(26 + vlan_offset);
		final short port = bb.getShort();
		// TODO: need to do this for LLDPs coming from physical switches
		// byte possibleSysId[] = new byte[2];
		// bb.position(32 + vlan_offset);
		// bb.get(possibleSysId);
		// if (!Arrays.equals(possibleSysId, TopologyConnection.lldpSysD))
		// return null;
		bb.position(34 + vlan_offset);
		final long dpid = bb.getLong();
		return new DPIDandPort(dpid, port);
	}

}
