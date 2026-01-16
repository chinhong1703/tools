package com.yourorg.objectstore.factory;

import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.ObjectStoreLimits;
import com.yourorg.objectstore.decorators.BoundedGetBytesObjectStore;
import com.yourorg.objectstore.decorators.PrefixedObjectStore;
import com.yourorg.objectstore.impl.local.LocalDiskObjectStore;
import com.yourorg.objectstore.impl.mem.InMemoryObjectStore;
import com.yourorg.objectstore.impl.s3.S3ObjectStore;
import software.amazon.awssdk.services.s3.S3Client;

public final class ObjectStoreFactory {
  private ObjectStoreFactory() {
  }

  public static ObjectStore inMemory(String prefix, ObjectStoreLimits limits) {
    return decorate(new InMemoryObjectStore(), prefix, limits);
  }

  public static ObjectStore localDisk(String storeName, String prefix, ObjectStoreLimits limits) {
    return decorate(new LocalDiskObjectStore(storeName), prefix, limits);
  }

  public static ObjectStore s3(S3Client client, String bucket, String prefix, ObjectStoreLimits limits) {
    return decorate(new S3ObjectStore(client, bucket), prefix, limits);
  }

  private static ObjectStore decorate(ObjectStore backend, String prefix, ObjectStoreLimits limits) {
    ObjectStore prefixed = new PrefixedObjectStore(backend, prefix);
    return new BoundedGetBytesObjectStore(prefixed, limits);
  }
}
