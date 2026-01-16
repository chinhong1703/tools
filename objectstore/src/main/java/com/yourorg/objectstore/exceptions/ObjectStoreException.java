package com.yourorg.objectstore.exceptions;

public class ObjectStoreException extends Exception {
  public ObjectStoreException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectStoreException(String message) {
    super(message);
  }
}
