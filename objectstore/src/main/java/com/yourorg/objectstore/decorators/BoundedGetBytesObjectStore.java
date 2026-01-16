package com.yourorg.objectstore.decorators;

import com.yourorg.objectstore.ListResult;
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.ObjectStoreLimits;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class BoundedGetBytesObjectStore implements ObjectStore {
  private final ObjectStore backend;
  private final ObjectStoreLimits limits;

  public BoundedGetBytesObjectStore(ObjectStore backend, ObjectStoreLimits limits) {
    this.backend = backend;
    this.limits = limits == null ? ObjectStoreLimits.defaults() : limits;
  }

  @Override
  public void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    backend.put(key, data, contentLength, options);
  }

  @Override
  public InputStream get(String key) throws ObjectNotFoundException, ObjectStoreException {
    return backend.get(key);
  }

  @Override
  public byte[] getBytes(String key) throws ObjectNotFoundException, ObjectStoreException {
    try (InputStream stream = backend.get(key)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[16 * 1024];
      long total = 0;
      int read;
      while ((read = stream.read(buffer)) != -1) {
        total += read;
        if (total > limits.maxGetBytes()) {
          throw new ObjectStoreException("Object exceeds maximum size of " + limits.maxGetBytes());
        }
        output.write(buffer, 0, read);
      }
      return output.toByteArray();
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to read object stream", e);
    }
  }

  @Override
  public Optional<InputStream> getIfExists(String key) throws ObjectStoreException {
    try {
      return Optional.of(backend.get(key));
    } catch (ObjectNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean exists(String key) throws ObjectStoreException {
    return backend.exists(key);
  }

  @Override
  public void delete(String key) throws ObjectStoreException {
    backend.delete(key);
  }

  @Override
  public ListResult list(String prefix, int limit, String continuationToken) throws ObjectStoreException {
    return backend.list(prefix, limit, continuationToken);
  }

  @Override
  public void close() throws ObjectStoreException {
    backend.close();
  }
}
