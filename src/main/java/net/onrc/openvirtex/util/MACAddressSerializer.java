package net.onrc.openvirtex.util;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MACAddressSerializer implements JsonSerializer<MACAddress> {

	@Override
	public JsonElement serialize(MACAddress mac, Type t,
			JsonSerializationContext c) {
		final JsonPrimitive res = new JsonPrimitive(mac.toString());
		return res;
	}
}
