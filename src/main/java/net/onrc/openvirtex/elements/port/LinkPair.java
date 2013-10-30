package net.onrc.openvirtex.elements.port;

/**
 * Class representing a link connected to a port. Made of 
 * two links - one with this port as source, and the other 
 * with this port as destination. 
 * 
 * @param T generic Link 
 */
public class LinkPair <T> {
    
    	/** link with port as destination  */
    	protected T ingressLink;
    	/** link with port as source */
    	protected T egressLink;
	    
    	public LinkPair() {
    	    	this.ingressLink = null;
		this.egressLink = null;
    	}
    	
    	public void setInLink(T link) {
    	    	this.ingressLink = link;
    	}
    	
    	public void setOutLink(T link) {
	    	this.egressLink = link;
	}
    	
    	/**
    	 * remove inbound link (sets it to null)
    	 */
    	public void removeInLink() {
    	    //should set opposing link accordingly
    	    	this.ingressLink = null;
    	}
    	
    	/**
    	 * Remove outbound link (sets it to null)
    	 */
    	public void removeOutLink() {
    	    //should set opposing link accordingly
    	    	this.egressLink = null;
	}
    	
    	/**
    	 * @return the link with this port as destination
    	 */
    	public T getInLink() {
    	    	return this.ingressLink;
    	}
    	
    	/**
    	 * @return the link with this port as source
    	 */
    	public T getOutLink() {
    	    	return this.egressLink;
    	}
    	
    	/**
    	 * @return false if both ingress and egress links are null
    	 */
    	public boolean exists() {
    	    	return ((this.ingressLink != null) && (this.egressLink != null));
    	}
    	
    	@Override
    	public String toString() {
    	    	return "LinkPair[" + this.ingressLink.toString() + 
    	    		":" + this.egressLink.toString() +"]";
    	}
}
