/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.port;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PhysicalPortSerializer implements JsonSerializer<PhysicalPort> {

	@Override
	public JsonElement serialize(final PhysicalPort port, final Type portType,
			final JsonSerializationContext context) {
		final JsonObject result = new JsonObject();
		result.addProperty("dpid", port.getParentSwitch().getSwitchName());
		result.addProperty("port", String.valueOf(port.getPortNumber()));
		return result;
	}

}
