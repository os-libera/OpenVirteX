package net.onrc.openvirtex.exceptions;

/**
 * Exception thrown when Components are in an unexpected state.
 */
public class ComponentStateException extends Exception {

    /*"OVX"*/
    private static final long serialVersionUID = 798688L;

    public ComponentStateException(Throwable cause) {
        super(cause);
    }

    public ComponentStateException(String cause) {
        super(cause);
    }

}
