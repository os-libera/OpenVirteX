/**
 *
 */
package net.onrc.openvirtex.exceptions;


public class MissingRequiredField extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public MissingRequiredField(String fieldName) {
		super(fieldName + " is required ");
	}

}
