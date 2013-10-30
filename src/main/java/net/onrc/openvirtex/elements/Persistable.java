package net.onrc.openvirtex.elements;

import java.util.Map;

import net.onrc.openvirtex.elements.link.Link;

public interface Persistable {
	public Map<String, Object> getDBIndex();

	public String getDBKey();
	
	public String getDBName();
	
	public Map<String, Object> getDBObject();
	
}
