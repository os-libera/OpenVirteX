package net.onrc.openvirtex.exceptions;

import net.onrc.openvirtex.elements.link.Link;

/**
 * Exception thrown when links are not found in the map. Links 
 * refer to OVXLink, PhysicalLink, and SwitchRoute.  
 */
public class LinkMappingException extends MappingException {

	private static final long serialVersionUID = 798688L;

	public LinkMappingException() {
		super();
	}
	
	public LinkMappingException(String cause) {
		super(cause);
	}
	
	public LinkMappingException(Integer key, Class value) {
	    	super(value.getName() + " not found for tenant with ID " + key);
	}

	public LinkMappingException(Link key, Class value) {
	    	super(value.getName() + " not found for Link [" + key + "]");
	}
	
	public LinkMappingException(Object key, Class value) {
		super(key, value);
	}
	
	public LinkMappingException(Throwable cause) {
		super(cause);
	}
	
}
