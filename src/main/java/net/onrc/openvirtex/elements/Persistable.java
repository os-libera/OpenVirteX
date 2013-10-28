package net.onrc.openvirtex.elements;

import java.util.Map;

public interface Persistable {
	public Map<String, Object> getDBIndex();

	public String getDBKey();
	
	public String getDBName();
	
	public Map<String, Object> getDBObject();
	
}
