/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;

import org.apache.commons.lang.ArrayUtils;

/**
 * LLDP packets OpenVirteX uses for discovery of physical network topology.
 * Refer to IEEE Std 802.1ABTM-2009 for more information.
 *
 */
@SuppressWarnings("rawtypes")
public class OVXLLDP extends LLDP {

    // ON.Lab OUI and OVX name for organizationally specific TLVs
    public static final byte[] ONLAB_OUI = { (byte) 0xa4, 0x23, 0x05 };
    public static final String OVX_NAME = "OpenVirteX";
    public static final byte[] LLDP_NICIRA = { 0x01, 0x23, 0x20, 0x00, 0x00,
        0x01 };
    public static final byte[] LLDP_MULTICAST = { 0x01, (byte) 0x80,
        (byte) 0xc2, 0x00, 0x00, 0x0e };
    public static final byte[] BDDP_MULTICAST = { (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
    public static final short ETHERTYPE_VLAN = (short) 0x8100;

    // TLV constants: type, size and subtype
    // Organizationally specific TLV also have packet offset and contents of TLV
    // header
    private static final byte CHASSIS_TLV_TYPE = 1;
    private static final byte CHASSIS_TLV_SIZE = 7;
    private static final byte CHASSIS_TLV_SUBTYPE = 4;

    private static final byte PORT_TLV_TYPE = 2;
    private static final byte PORT_TLV_SIZE = 3;
    private static final byte PORT_TLV_SUBTYPE = 2;

    private static final byte TTL_TLV_TYPE = 3;
    private static final byte TTL_TLV_SIZE = 2;

    private static final byte NAME_TLV_TYPE = 127;
    // 4 = OUI (3) + subtype (1)
    private static final byte NAME_TLV_SIZE = (byte) (4 + OVXLLDP.OVX_NAME
            .length());
    private static final byte NAME_TLV_SUBTYPE = 1;
    private static final short NAME_TLV_OFFSET = 32;
    private static final short NAME_TLV_HEADER = (short) (OVXLLDP.NAME_TLV_TYPE << 9 | OVXLLDP.NAME_TLV_SIZE);
    // Contents of full name TLV
    private static final byte[] NAME_TLV = ByteBuffer
            .allocate(OVXLLDP.NAME_TLV_SIZE + 2)
			.putShort(OVXLLDP.NAME_TLV_HEADER).put(OVXLLDP.ONLAB_OUI)
			.put(OVXLLDP.NAME_TLV_SUBTYPE).put(OVXLLDP.OVX_NAME.getBytes())
            .array();

    private static final byte DPID_TLV_TYPE = 127;
    private static final byte DPID_TLV_SIZE = (byte) 12; // 12 = OUI (3) +
    // subtype
    // (1) + dpid (8)
    private static final byte DPID_TLV_SUBTYPE = 2;
    private static final short DPID_TLV_HEADER = (short) (OVXLLDP.DPID_TLV_TYPE << 9 | OVXLLDP.DPID_TLV_SIZE);
    // Contents of dpid TLV
    // Note that this does *not* contain the actual dpid since we cannot match
    // on it
    private static final byte[] DPID_TLV = ByteBuffer
            .allocate(OVXLLDP.DPID_TLV_SIZE + 2 - 8)
			.putShort(OVXLLDP.DPID_TLV_HEADER).put(OVXLLDP.ONLAB_OUI)
			.put(OVXLLDP.DPID_TLV_SUBTYPE).array();

    // Pre-built contents of both organizationally specific TLVs
    private static final byte[] OUI_TLV = ArrayUtils.addAll(OVXLLDP.NAME_TLV,
			OVXLLDP.DPID_TLV);

    // Default switch, port number and TTL
    private static final byte[] DEFAULT_DPID = { 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00 };
    private static final short DEFAULT_PORT = 0;
    private static final short DEFAULT_TTL = 120; // in seconds

    // Minimum and OVX-generated LLDP packet sizes
    private static final short MINIMUM_LLDP_SIZE = 61;
    // Add 12 for 2-byte header of each TLV and a single EndOfLLDPTLV
    private static final short OVX_LLDP_SIZE = (short) (OVXLLDP.CHASSIS_TLV_SIZE
            + OVXLLDP.PORT_TLV_SIZE
			+ OVXLLDP.TTL_TLV_SIZE
			+ OVXLLDP.NAME_TLV_SIZE + OVXLLDP.DPID_TLV_SIZE + 12);

    // Field offsets in OVX-generated LLDP
    private static final short ETHERTYPE_OFFSET = 12;
    private static final short PORT_OFFSET = 26;
    private static final short DPID_OFFSET = 54;

    // Private member fields
    // Byte arrays for TLV information string
    private ByteBuffer bb;
    private final byte[] chassisId = new byte[OVXLLDP.CHASSIS_TLV_SIZE];
    private final byte[] portId = new byte[OVXLLDP.PORT_TLV_SIZE];
    private final byte[] ttl = new byte[OVXLLDP.TTL_TLV_SIZE];
    private final byte[] ouiName = new byte[OVXLLDP.NAME_TLV_SIZE];
    private final byte[] ouiDpid = new byte[OVXLLDP.DPID_TLV_SIZE];

    // TLVs
    private final LLDPTLV chassisTLV;
    private final LLDPTLV portTLV;
    private final LLDPTLV ttlTLV;
    private final LLDPTLV ouiNameTLV;
    private final LLDPTLV ouiDpidTLV;
    private final List<LLDPTLV> optionalTLVList;

    private Switch sw = null;
    private Port port = null;

    /**
     * Instantiates a new OVX LDDP message.
     */
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
        this.setChassisTLV(OVXLLDP.DEFAULT_DPID);
        this.setPortTLV(OVXLLDP.DEFAULT_PORT);
        this.setTTLTLV(OVXLLDP.DEFAULT_TTL);
        this.setOUIName(OVXLLDP.OVX_NAME);
        this.setOUIDpid(OVXLLDP.DEFAULT_DPID);
    }

    /**
     * Sets chassis TLV. Note that we can only put 6 bytes in the chassis ID, so
     * we use another organizationally specific TLV to put the full dpid (see
     * setOUIDpid()).
     *
     * @param dpid
     *            the switch DPID
     */
    private void setChassisTLV(final byte[] dpid) {
        this.bb = ByteBuffer.wrap(this.chassisId);
        this.bb.put(OVXLLDP.CHASSIS_TLV_SUBTYPE);
        for (int i = 2; i < 8; i++) {
            this.bb.put(dpid[i]);
        }
        this.chassisTLV.setLength(OVXLLDP.CHASSIS_TLV_SIZE);
        this.chassisTLV.setType(OVXLLDP.CHASSIS_TLV_TYPE);
        this.chassisTLV.setValue(this.chassisId);
    }

    /**
     * Sets port TLV.
     *
     * @param portNumber
     *            the port number
     */
    private void setPortTLV(final short portNumber) {
        this.bb = ByteBuffer.wrap(this.portId);
        this.bb.put(OVXLLDP.PORT_TLV_SUBTYPE);
        this.bb.putShort(portNumber);

        this.portTLV.setLength(OVXLLDP.PORT_TLV_SIZE);
        this.portTLV.setType(OVXLLDP.PORT_TLV_TYPE);
        this.portTLV.setValue(this.portId);
    }

    /**
     * Sets Time To Live TLV.
     *
     * @param time
     *            the time to live
     */
    private void setTTLTLV(final short time) {
        this.bb = ByteBuffer.wrap(this.ttl);
        this.bb.putShort(time);

        this.ttlTLV.setLength(OVXLLDP.TTL_TLV_SIZE);
        this.ttlTLV.setType(OVXLLDP.TTL_TLV_TYPE);
        this.ttlTLV.setValue(this.ttl);
    }

    /**
     * Set. organizationally specific TLV for OVX name (subtype 1).
     *
     * @param name
     *            the name
     */
    private void setOUIName(final String name) {
        this.bb = ByteBuffer.wrap(this.ouiName);
        this.bb.put(OVXLLDP.ONLAB_OUI);
        this.bb.put(OVXLLDP.NAME_TLV_SUBTYPE);
        this.bb.put(name.getBytes());

        this.ouiNameTLV.setLength(OVXLLDP.NAME_TLV_SIZE);
        this.ouiNameTLV.setType(OVXLLDP.NAME_TLV_TYPE);
        this.ouiNameTLV.setValue(this.ouiName);
    }

    /**
     * Sets organizationally specific TLV for OVX full dpid (subtype 2).
     *
     * @param dpid
     *            the switch DPID
     */
    private void setOUIDpid(final byte[] dpid) {
        this.bb = ByteBuffer.wrap(this.ouiDpid);
        this.bb.put(OVXLLDP.ONLAB_OUI);
        this.bb.put(OVXLLDP.DPID_TLV_SUBTYPE);
        this.bb.put(dpid);

        this.ouiDpidTLV.setLength(OVXLLDP.DPID_TLV_SIZE);
        this.ouiDpidTLV.setType(OVXLLDP.DPID_TLV_TYPE);
        this.ouiDpidTLV.setValue(this.ouiDpid);
    }

    /**
     * Sets switch DPID in LLDP packet.
     *
     * @param sw
     *            the switch instance
     */
    public void setSwitch(final Switch sw) {
        this.sw = sw;
        final byte[] dpid = ByteBuffer.allocate(8).putLong(sw.getSwitchId())
                .array();
        this.setChassisTLV(dpid);
        this.setOUIDpid(dpid);
    }

    /**
     * Sets port in LLDP packet.
     *
     * @param port
     *            the port instance
     */
    public void setPort(final Port port) {
        this.port = port;
        final short portNumber = this.port.getPortNumber();
        this.setPortTLV(portNumber);
    }

    /**
     * Serializes full LLDP packet to byte array. Need to set both switch and
     * port before you can serialize.
     */
    @Override
    public byte[] serialize() {
        if (this.sw == null || this.port == null) {
            return null;
        }
        return super.serialize();
    }

    /**
     * Checks if LLDP packet has correct size, LLDP multicast address, and
     * ethertype. Packet assumed to have Ethernet header.
     *
     * @param packet
     * @return true if packet is LLDP, false otherwise
     */
    public static boolean isLLDP(final byte[] packet) {
        // Does packet exist and does it have the mininum size?
        if (packet == null || packet.length < OVXLLDP.MINIMUM_LLDP_SIZE) {
            return false;
        }

        // Packet has LLDP multicast destination address?
        final ByteBuffer bb = ByteBuffer.wrap(packet);
        final byte[] dst = new byte[6];
        bb.get(dst);

        if (!(Arrays.equals(dst, OVXLLDP.LLDP_NICIRA)
                || Arrays.equals(dst, OVXLLDP.LLDP_MULTICAST) || Arrays.equals(
                        dst, OVXLLDP.BDDP_MULTICAST))) {

            return false;
        }

        // Fetch ethertype, skip VLAN tag if it's there
        short etherType = bb.getShort(OVXLLDP.ETHERTYPE_OFFSET);
        if (etherType == OVXLLDP.ETHERTYPE_VLAN) {
            etherType = bb.getShort(OVXLLDP.ETHERTYPE_OFFSET + 4);
        }

        // Check ethertype
        if (etherType == Ethernet.TYPE_LLDP) {
            return true;
        }
        if (etherType == Ethernet.TYPE_BSN) {
            return true;
        }

        return false;

    }

    /**
     * Checks if packet has size of OVX-generated LLDP, and correctness of two
     * organizationally specific TLVs that use ON.Lab's OUI. Assumes packet is
     * valid LLDP packet
     *
     * @param packet
     * @return
     */
    public static boolean isOVXLLDP(final byte[] packet) {
        if (packet.length < OVXLLDP.OVX_LLDP_SIZE) {
            return false;
        }

        // Extra offset due to VLAN tag
        final ByteBuffer bb = ByteBuffer.wrap(packet);
        int offset = 0;
        if (bb.getShort(OVXLLDP.ETHERTYPE_OFFSET) != Ethernet.TYPE_LLDP
                && bb.getShort(OVXLLDP.ETHERTYPE_OFFSET) != Ethernet.TYPE_BSN) {
            offset = 4;
        }

        // Compare packet's organizationally specific TLVs to the expected
        // values
        for (int i = 0; i < OVXLLDP.OUI_TLV.length; i++) {
            if (packet[OVXLLDP.NAME_TLV_OFFSET + offset + i] != OVXLLDP.OUI_TLV[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts dpid and port from OVX-generated LLDP packet.
     *
     * @param packet
     * @return Dpid and port
     */
    public static DPIDandPort parseLLDP(final byte[] packet) {
        final ByteBuffer bb = ByteBuffer.wrap(packet);

        // Extra offset due to VLAN tag
        int offset = 0;
        if (bb.getShort(OVXLLDP.ETHERTYPE_OFFSET) != Ethernet.TYPE_LLDP
                && bb.getShort(OVXLLDP.ETHERTYPE_OFFSET) != Ethernet.TYPE_BSN) {
            offset = 4;
        }

        final short port = bb.getShort(OVXLLDP.PORT_OFFSET + offset);
        final long dpid = bb.getLong(OVXLLDP.DPID_OFFSET + offset);

        return new DPIDandPort(dpid, port);
    }
}
