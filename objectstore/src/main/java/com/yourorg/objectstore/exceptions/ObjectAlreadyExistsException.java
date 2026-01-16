package com.yourorg.objectstore.exceptions;

public final class ObjectAlreadyExistsException extends ObjectStoreException {
  public ObjectAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectAlreadyExistsException(String message) {
    super(message);
  }
}
