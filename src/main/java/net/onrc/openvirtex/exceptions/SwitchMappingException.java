package net.onrc.openvirtex.exceptions;

import net.onrc.openvirtex.elements.datapath.Switch;


/**
 * Exception thrown when OVX/Physical switches or their attributes (ports) are not found 
 * in a map. 
 */
public class SwitchMappingException extends MappingException {
    
	private static final long serialVersionUID = 798688L;
	
	public SwitchMappingException() {
		super();
	}
	
	public SwitchMappingException(String cause) {
		super(cause);
	}
	
	public SwitchMappingException(Integer key, Class value) {
	    	super(value.getName() + " not found for tenant with ID " + key);
	}
	
	public SwitchMappingException(Switch key, Class value) {
	    	super(value.getName() + " not found for switch [" + key.getSwitchId() + "]");
	}
	
	public SwitchMappingException(Object key, Class value) {
		super(key, value);
	}

	public SwitchMappingException(Throwable cause) {
		super(cause);
	}
	
}
