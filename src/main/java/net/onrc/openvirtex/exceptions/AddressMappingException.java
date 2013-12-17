package net.onrc.openvirtex.exceptions;

import net.onrc.openvirtex.elements.address.IPAddress;


/**
 * Exception thrown when addresses are not found in mappings. Addresses
 * include IP(virtual and physical) and hardware addresses (MACs).  
 */
@SuppressWarnings("rawtypes")
public class AddressMappingException extends MappingException {
    
	private static final long serialVersionUID = 798688L;
	
	public AddressMappingException() {    
		super();
	}
	
	public AddressMappingException(String cause) {
		super(cause);
	}

	public AddressMappingException(Integer key, Class value) {
	    	super(value.getName() + " not found for tenant with ID " + key);
	}

	public AddressMappingException(IPAddress key, Class value) {
	    	super(value.getName() + " not found for " + key);
	}
	
	public AddressMappingException(Throwable cause) {
		super(cause);
	}

}
