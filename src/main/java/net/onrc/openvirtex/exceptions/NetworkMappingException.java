package net.onrc.openvirtex.exceptions;


/**
 * Exception thrown when tenant networks are not found in the map. 
 */
@SuppressWarnings("rawtypes")
public class NetworkMappingException extends MappingException {
    
	private static final long serialVersionUID = 798688L;
	
	public NetworkMappingException() {
		super();
	}
	
	public NetworkMappingException(String cause) {
		super(cause);
	}
	
	public NetworkMappingException(Integer key) {
	    	super("Virtual network not found for tenant with ID " + key);
	}
	
	public NetworkMappingException(Object key, Class value) {
		super(key, value);
	}

	public NetworkMappingException(Throwable cause) {
		super(cause);
	}

}
