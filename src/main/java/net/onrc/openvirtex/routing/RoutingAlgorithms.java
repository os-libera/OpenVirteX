/**
 * 
 */
package net.onrc.openvirtex.routing;

/**
 * @author gerola
 * 
 */
public enum RoutingAlgorithms {
	NONE((short) 0), SFP((short) 1);

	protected short value;
	/** the routable */
	protected Routable routing;
	
	/**the types of routable mapping to each*/
	static Routable [] routingmap;

	private RoutingAlgorithms(final short value) {
		this.value = value; 
		RoutingAlgorithms.setRoutable(this.value, this);
	}

	private static void setRoutable(Short value, RoutingAlgorithms algo) {
	    if (routingmap == null) {
		routingmap = new Routable[2];
		routingmap[0] = new ManualRoute();
		routingmap[1] = new ShortestPath();		
	    }
	    algo.routing = routingmap[value];
        }

	/**
	 * @return the value
	 */
	public short getValue() {
	    return this.value;
	}
	
	/**
	 * @return the Routable associated with the algorithm
	 */
	public Routable getRoutable() {
	    return this.routing;
	}
}