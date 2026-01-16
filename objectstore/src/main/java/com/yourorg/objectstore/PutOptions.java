package com.yourorg.objectstore;

import java.util.Map;

public record PutOptions(String contentType, Map<String, String> userMetadata) {

  public PutOptions {
    if (userMetadata == null) {
      throw new IllegalArgumentException("userMetadata must not be null");
    }
  }

  public static PutOptions defaults() {
    return new PutOptions(null, Map.of());
  }
}
