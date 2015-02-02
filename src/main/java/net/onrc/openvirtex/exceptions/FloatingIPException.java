package net.onrc.openvirtex.exceptions;

public class FloatingIPException extends Exception{

    private static final long serialVersionUID = 798688L;

    public FloatingIPException(String cause) {
        super(cause);
    }

    public FloatingIPException(String message, Throwable cause) {
        super(message, cause);
    }
}
