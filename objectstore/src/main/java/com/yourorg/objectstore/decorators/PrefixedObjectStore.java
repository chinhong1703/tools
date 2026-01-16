package com.yourorg.objectstore.decorators;

import com.yourorg.objectstore.ListResult;
import com.yourorg.objectstore.ObjectInfo;
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import com.yourorg.objectstore.key.KeyTransformer;
import com.yourorg.objectstore.key.KeyValidator;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public final class PrefixedObjectStore implements ObjectStore {
  private final ObjectStore backend;
  private final String normalizedPrefix;

  public PrefixedObjectStore(ObjectStore backend, String prefix) {
    this.backend = backend;
    this.normalizedPrefix = KeyTransformer.normalizePrefix(prefix);
  }

  @Override
  public void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    backend.put(KeyTransformer.addPrefix(normalizedPrefix, key), data, contentLength, options);
  }

  @Override
  public InputStream get(String key) throws ObjectNotFoundException, ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    return backend.get(KeyTransformer.addPrefix(normalizedPrefix, key));
  }

  @Override
  public byte[] getBytes(String key) throws ObjectNotFoundException, ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    return backend.getBytes(KeyTransformer.addPrefix(normalizedPrefix, key));
  }

  @Override
  public Optional<InputStream> getIfExists(String key) throws ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    return backend.getIfExists(KeyTransformer.addPrefix(normalizedPrefix, key));
  }

  @Override
  public boolean exists(String key) throws ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    return backend.exists(KeyTransformer.addPrefix(normalizedPrefix, key));
  }

  @Override
  public void delete(String key) throws ObjectStoreException {
    KeyValidator.validateLogicalKey(key, normalizedPrefix);
    backend.delete(KeyTransformer.addPrefix(normalizedPrefix, key));
  }

  @Override
  public ListResult list(String prefix, int limit, String continuationToken) throws ObjectStoreException {
    KeyValidator.validateLogicalPrefix(prefix, normalizedPrefix);
    KeyValidator.validateLimit(limit);
    String physicalPrefix = KeyTransformer.addPrefix(normalizedPrefix, prefix);
    String physicalToken = continuationToken == null || continuationToken.isBlank()
        ? null
        : KeyTransformer.addPrefix(normalizedPrefix, continuationToken);
    ListResult result = backend.list(physicalPrefix, limit, physicalToken);
    List<ObjectInfo> logicalItems = result.items().stream()
        .map(item -> new ObjectInfo(
            KeyTransformer.stripPrefix(normalizedPrefix, item.key()),
            item.sizeBytes(),
            item.lastModified()))
        .toList();
    String nextToken = result.nextContinuationToken();
    if (nextToken != null && !nextToken.isBlank()) {
      nextToken = KeyTransformer.stripPrefix(normalizedPrefix, nextToken);
    }
    return new ListResult(logicalItems, nextToken);
  }

  @Override
  public void close() throws ObjectStoreException {
    backend.close();
  }
}
