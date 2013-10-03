/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.openflow.protocol.statistics;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * The base class for vendor implemented statistics
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFVendorStatistics implements OFStatistics {
	protected int vendor;
	protected byte[] body;

	// non-message fields
	protected int length = 0;

	@Override
	public void readFrom(final ChannelBuffer data) {
		this.vendor = data.readInt();
		if (this.body == null) {
			this.body = new byte[this.length - 4];
		}
		data.readBytes(this.body);
	}

	@Override
	public void writeTo(final ChannelBuffer data) {
		data.writeInt(this.vendor);
		if (this.body != null) {
			data.writeBytes(this.body);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 457;
		int result = 1;
		result = prime * result + this.vendor;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof OFVendorStatistics)) {
			return false;
		}
		final OFVendorStatistics other = (OFVendorStatistics) obj;
		if (this.vendor != other.vendor) {
			return false;
		}
		return true;
	}

	@Override
	public int getLength() {
		return this.length;
	}

	public void setLength(final int length) {
		this.length = length;
	}
}
