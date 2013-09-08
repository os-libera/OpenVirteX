package net.onrc.openvirtex.elements.port;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PhysicalPortSerializer implements JsonSerializer<PhysicalPort> {

    @Override
    public JsonElement serialize(PhysicalPort port, Type portType,
            JsonSerializationContext context) {
	JsonObject result = new JsonObject();
	result.addProperty("dpid", port.getParentSwitch().getSwitchId());
	result.addProperty("port", port.getPortNumber());
	return result;
    }

}
