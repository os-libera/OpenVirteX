/**
 * 
 */
package net.onrc.openvirtex.elements.link;

/**
 * @author gerola
 *
 */
public enum OVXLinkField {
	MAC_ADDRESS((byte) 0), 
	VLAN((byte) 1);

	protected byte value;

	private OVXLinkField(byte value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public byte getValue() {
		return value;
	}
}
