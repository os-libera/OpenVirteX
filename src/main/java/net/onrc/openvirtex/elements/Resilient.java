package net.onrc.openvirtex.elements;

/**
 * The methods that a Component must implement if capable of recovering 
 * from subcomponent failure. A Resilient class is always a Component. 
 */
public interface Resilient {
	/**
	 * Try to recover from the failure of c, e.g. a PhysicalLink
	 * goes down.
	 * @param c Component that had failed
	 * @return true if successful.
	 */
	public boolean tryRecovery(Component c);
	
	/**
	 * Actions taken when c returns to a functional state
	 * e.g. a PhysicalLink comes back up.
	 * @param c Component returning from failed state
	 * @return true if successful.
	 */
	public boolean tryRevert(Component c);
}
