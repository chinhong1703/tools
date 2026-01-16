package com.yourorg.objectstore.key;

public final class KeyTransformer {
  private KeyTransformer() {
  }

  public static String normalizePrefix(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return "";
    }
    String trimmed = prefix.trim();
    return trimmed.endsWith("/") ? trimmed : trimmed + "/";
  }

  public static String addPrefix(String normalizedPrefix, String key) {
    if (normalizedPrefix == null || normalizedPrefix.isEmpty()) {
      return key;
    }
    return normalizedPrefix + key;
  }

  public static String stripPrefix(String normalizedPrefix, String key) {
    if (normalizedPrefix == null || normalizedPrefix.isEmpty()) {
      return key;
    }
    if (key.startsWith(normalizedPrefix)) {
      return key.substring(normalizedPrefix.length());
    }
    return key;
  }
}
