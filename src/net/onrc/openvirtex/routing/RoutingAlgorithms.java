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

    private RoutingAlgorithms(final short value) {
	this.value = value;
    }

    /**
     * @return the value
     */
    public short getValue() {
	return this.value;
    }
}
