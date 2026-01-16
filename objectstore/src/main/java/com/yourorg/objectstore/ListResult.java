package com.yourorg.objectstore;

import java.util.List;

public record ListResult(List<ObjectInfo> items, String nextContinuationToken) {}
