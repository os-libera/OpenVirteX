/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class OVXSwitchSerializer implements JsonSerializer<OVXSwitch> {

	@Override
	public JsonElement serialize(final OVXSwitch sw,
			final Type switchType, final JsonSerializationContext context) {
		final JsonPrimitive dpid = new JsonPrimitive(sw.switchName);
		return dpid;
	}

}
