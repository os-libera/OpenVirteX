package net.onrc.openvirtex.exceptions;

/**
 * An exception thrown when inconsistency is found in *Port-related mappings. 
 */
public class PortMappingException extends MappingException {
    private static final long serialVersionUID = 798688L;
	
    public PortMappingException() {
	super();
    }
    
    public PortMappingException(String cause) {
	super(cause);
    }
    
    public PortMappingException(Throwable cause) {
	super(cause);
    }
    
}
