package com.yourorg.objectstore;

public record ObjectStoreLimits(long maxGetBytes) {
  public static ObjectStoreLimits defaults() {
    return new ObjectStoreLimits(32L * 1024 * 1024);
  }
}
