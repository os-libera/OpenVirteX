/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.host;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class HostSerializer implements JsonSerializer<Host> {

	@Override
	public JsonElement serialize(Host host, Type t,
			JsonSerializationContext c) {
		final JsonObject result = new JsonObject();
		result.addProperty("hostId", host.getHostId());
		result.addProperty("ipAddress", host.getIp().toSimpleString());
		result.addProperty("mac", host.getMac().toString());
		result.addProperty("dpid", host.getPort().getParentSwitch().getSwitchName());
		result.addProperty("port", host.getPort().getPortNumber());
		return result;
	}
}
