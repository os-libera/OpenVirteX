package net.onrc.openvirtex.exceptions;

/**
 * Each virtual network should talk to a different controller. If the admin tries to make to virtual networks talk to the same controller
 * then this exception will be thrown.
 */
public class ControllerUnavailableException extends Exception {

	public ControllerUnavailableException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
