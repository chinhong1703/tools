package com.yourorg.objectstore.exceptions;

public final class ObjectNotFoundException extends ObjectStoreException {
  public ObjectNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectNotFoundException(String message) {
    super(message);
  }
}
