package net.onrc.openvirtex.elements.host;

import java.lang.reflect.Type;

import net.onrc.openvirtex.elements.port.OVXPort;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class HostSerializer implements JsonSerializer<Host> {

	@Override
	public JsonElement serialize(Host host, Type t,
			JsonSerializationContext c) {
		final JsonObject result = new JsonObject();
		result.addProperty("mac", host.getMac().toString());
		result.addProperty("dpid", host.getPort().getParentSwitch().getSwitchName());
		result.addProperty("port", host.getPort().getPortNumber());
		return result;
	}
}
