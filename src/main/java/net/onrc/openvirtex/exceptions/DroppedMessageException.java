/**
 * 
 */
package net.onrc.openvirtex.exceptions;

/**
 * @author gerola
 *
 */
public class DroppedMessageException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 2086213976572923879L;

    public DroppedMessageException() {
  	super();
      }

      public DroppedMessageException(String msg) {
  	super(msg);
      }

      public DroppedMessageException(Throwable msg) {
  	super(msg);
      }
}
