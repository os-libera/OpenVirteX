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
/**
 * Copyright 2011, Big Switch Networks, Inc.
 * Originally created by David Erickson, Stanford University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 **/

package net.onrc.openvirtex.packet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class UDP extends AbstractSegment {
	public static Map<Short, Class<? extends IPacket>> decodeMap;
	public static short DHCP_SERVER_PORT = (short) 67;
	public static short DHCP_CLIENT_PORT = (short) 68;

	protected short length;
	static {
		UDP.decodeMap = new HashMap<Short, Class<? extends IPacket>>();
		/*
		 * Disable DHCP until the deserialize code is hardened to deal with
		 * garbage input
		 */
		UDP.decodeMap.put(UDP.DHCP_SERVER_PORT, DHCP.class);
		UDP.decodeMap.put(UDP.DHCP_CLIENT_PORT, DHCP.class);

	}

	/**
	 * @return the length
	 */
	public short getLength() {
		return this.length;
	}

	/**
	 * Serializes the packet. Will compute and set the following fields if they
	 * are set to specific values at the time serialize is called: -checksum : 0
	 * -length : 0
	 */
	@Override
	public byte[] serialize() {
		byte[] payloadData = null;
		if (this.payload != null) {
			this.payload.setParent(this);
			payloadData = this.payload.serialize();
		}

		this.length = (short) (8 + (payloadData == null ? 0
				: payloadData.length));

		final byte[] data = new byte[this.length];
		final ByteBuffer bb = ByteBuffer.wrap(data);

		bb.putShort(this.sourcePort);
		bb.putShort(this.destinationPort);
		bb.putShort(this.length);
		bb.putShort(this.checksum);
		if (payloadData != null) {
			bb.put(payloadData);
		}
		if (this.parent != null && this.parent instanceof IPv4) {
			((IPv4) this.parent).setProtocol(IPv4.PROTOCOL_UDP);
		}

		super.serialize(bb, this.length);
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 5807;
		int result = super.hashCode(prime);
		result = prime * result + this.length;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof UDP)) {
			return false;
		}
		final UDP other = (UDP) obj;
		if (this.checksum != other.checksum) {
			return false;
		}
		if (this.destinationPort != other.destinationPort) {
			return false;
		}
		if (this.length != other.length) {
			return false;
		}
		if (this.sourcePort != other.sourcePort) {
			return false;
		}
		return true;
	}

	@Override
	public IPacket deserialize(final byte[] data, final int offset,
			final int length) {
		final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		this.sourcePort = bb.getShort();
		this.destinationPort = bb.getShort();
		this.length = bb.getShort();
		this.checksum = bb.getShort();

		if (UDP.decodeMap.containsKey(this.destinationPort)) {
			try {
				this.payload = UDP.decodeMap.get(this.destinationPort)
						.getConstructor().newInstance();
			} catch (final Exception e) {
				throw new RuntimeException("Failure instantiating class", e);
			}
		} else if (UDP.decodeMap.containsKey(this.sourcePort)) {
			try {
				this.payload = UDP.decodeMap.get(this.sourcePort)
						.getConstructor().newInstance();
			} catch (final Exception e) {
				throw new RuntimeException("Failure instantiating class", e);
			}
		} else {
			this.payload = new Data();
		}
		this.payload = this.payload.deserialize(data, bb.position(), bb.limit()
				- bb.position());
		this.payload.setParent(this);
		return this;
	}

}
