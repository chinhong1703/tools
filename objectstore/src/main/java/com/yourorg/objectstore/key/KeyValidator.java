package com.yourorg.objectstore.key;

public final class KeyValidator {
  private KeyValidator() {
  }

  public static void validateLogicalKey(String key, String normalizedPrefix) {
    validateKeyOrPrefix(key, "key", normalizedPrefix);
  }

  public static void validateLogicalPrefix(String prefix, String normalizedPrefix) {
    validateKeyOrPrefix(prefix, "prefix", normalizedPrefix);
  }

  public static void validateLimit(int limit) {
    if (limit < 1 || limit > 1000) {
      throw new IllegalArgumentException("limit must be between 1 and 1000");
    }
  }

  private static void validateKeyOrPrefix(String value, String label, String normalizedPrefix) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    if (value.startsWith("/")) {
      throw new IllegalArgumentException(label + " must not start with /");
    }
    if (value.contains("\\")) {
      throw new IllegalArgumentException(label + " must not contain \\");
    }
    if (normalizedPrefix != null && !normalizedPrefix.isEmpty() && value.startsWith(normalizedPrefix)) {
      throw new IllegalArgumentException(label + " must not start with the configured prefix");
    }
    for (String segment : value.split("/", -1)) {
      if (segment.equals("..")) {
        throw new IllegalArgumentException(label + " must not contain .. segments");
      }
    }
  }
}
