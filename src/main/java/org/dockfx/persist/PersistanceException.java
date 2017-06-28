package org.dockfx.persist;

/**
 * Created by jding on 14/06/2017.
 */

/**
 * It should be extends from exception. However throw exception has problem in overriding.
 * Need to update if wrapping other framework
 */
public class PersistanceException extends Exception {
  public PersistanceException(String message) {
    super(message);
  }
}
