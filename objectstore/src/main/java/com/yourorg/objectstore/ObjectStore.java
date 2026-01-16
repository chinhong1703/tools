package com.yourorg.objectstore;

import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

public interface ObjectStore extends AutoCloseable {

  void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException;

  default void put(String key, byte[] data, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    put(key, new ByteArrayInputStream(data), data.length, options);
  }

  InputStream get(String key) throws ObjectNotFoundException, ObjectStoreException;

  byte[] getBytes(String key) throws ObjectNotFoundException, ObjectStoreException;

  Optional<InputStream> getIfExists(String key) throws ObjectStoreException;

  boolean exists(String key) throws ObjectStoreException;

  void delete(String key) throws ObjectStoreException;

  ListResult list(String prefix, int limit, String continuationToken) throws ObjectStoreException;

  @Override
  void close() throws ObjectStoreException;
}
