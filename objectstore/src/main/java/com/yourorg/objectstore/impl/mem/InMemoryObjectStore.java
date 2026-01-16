package com.yourorg.objectstore.impl.mem;

import com.yourorg.objectstore.ListResult;
import com.yourorg.objectstore.ObjectInfo;
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryObjectStore implements ObjectStore {
  private final ConcurrentHashMap<String, StoredObject> objects = new ConcurrentHashMap<>();

  @Override
  public void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    byte[] bytes = readAll(data);
    StoredObject stored = new StoredObject(bytes, options, Instant.now());
    StoredObject existing = objects.putIfAbsent(key, stored);
    if (existing != null) {
      throw new ObjectAlreadyExistsException("Object already exists: " + key);
    }
  }

  @Override
  public InputStream get(String key) throws ObjectNotFoundException {
    StoredObject stored = objects.get(key);
    if (stored == null) {
      throw new ObjectNotFoundException("Object not found: " + key);
    }
    return new ByteArrayInputStream(stored.data());
  }

  @Override
  public byte[] getBytes(String key) throws ObjectNotFoundException {
    StoredObject stored = objects.get(key);
    if (stored == null) {
      throw new ObjectNotFoundException("Object not found: " + key);
    }
    return stored.data();
  }

  @Override
  public Optional<InputStream> getIfExists(String key) {
    StoredObject stored = objects.get(key);
    if (stored == null) {
      return Optional.empty();
    }
    return Optional.of(new ByteArrayInputStream(stored.data()));
  }

  @Override
  public boolean exists(String key) {
    return objects.containsKey(key);
  }

  @Override
  public void delete(String key) {
    objects.remove(key);
  }

  @Override
  public ListResult list(String prefix, int limit, String continuationToken) {
    com.yourorg.objectstore.key.KeyValidator.validateLimit(limit);
    List<String> keys = new ArrayList<>();
    for (String key : objects.keySet()) {
      if (key.startsWith(prefix)) {
        keys.add(key);
      }
    }
    keys.sort(Comparator.naturalOrder());

    int startIndex = 0;
    if (continuationToken != null && !continuationToken.isBlank()) {
      for (int i = 0; i < keys.size(); i++) {
        if (keys.get(i).compareTo(continuationToken) > 0) {
          startIndex = i;
          break;
        }
        startIndex = keys.size();
      }
    }

    List<ObjectInfo> items = new ArrayList<>();
    int endIndex = Math.min(startIndex + limit, keys.size());
    for (int i = startIndex; i < endIndex; i++) {
      String key = keys.get(i);
      StoredObject stored = objects.get(key);
      items.add(new ObjectInfo(key, stored.data().length, stored.lastModified()));
    }
    String nextToken = endIndex < keys.size() ? keys.get(endIndex - 1) : null;
    return new ListResult(items, nextToken);
  }

  @Override
  public void close() {
  }

  private byte[] readAll(InputStream data) throws ObjectStoreException {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      data.transferTo(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to read input stream", e);
    }
  }

  private record StoredObject(byte[] data, PutOptions options, Instant lastModified) {
    private StoredObject {
      data = data.clone();
    }

    @Override
    public byte[] data() {
      return data.clone();
    }
  }
}
