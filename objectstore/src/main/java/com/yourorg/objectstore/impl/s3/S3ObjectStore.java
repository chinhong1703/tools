package com.yourorg.objectstore.impl.s3;

import com.yourorg.objectstore.ListResult;
import com.yourorg.objectstore.ObjectInfo;
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.core.overrideconfiguration.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3ObjectStore implements ObjectStore {
  private final S3Client client;
  private final String bucket;

  public S3ObjectStore(S3Client client, String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(options.contentType())
        .metadata(options.userMetadata())
        .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
            .putHeader("If-None-Match", "*")
            .build());
    try {
      client.putObject(requestBuilder.build(), RequestBody.fromInputStream(data, contentLength));
    } catch (S3Exception e) {
      if (e.statusCode() == 412) {
        throw new ObjectAlreadyExistsException("Object already exists: " + key, e);
      }
      throw new ObjectStoreException("Failed to put object: " + key, e);
    }
  }

  @Override
  public InputStream get(String key) throws ObjectNotFoundException, ObjectStoreException {
    try {
      return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (NoSuchKeyException e) {
      throw new ObjectNotFoundException("Object not found: " + key, e);
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        throw new ObjectNotFoundException("Object not found: " + key, e);
      }
      throw new ObjectStoreException("Failed to get object: " + key, e);
    }
  }

  @Override
  public byte[] getBytes(String key) throws ObjectNotFoundException, ObjectStoreException {
    try (InputStream stream = get(key)) {
      return stream.readAllBytes();
    } catch (java.io.IOException e) {
      throw new ObjectStoreException("Failed to read object: " + key, e);
    }
  }

  @Override
  public java.util.Optional<InputStream> getIfExists(String key) throws ObjectStoreException {
    try {
      return java.util.Optional.of(get(key));
    } catch (ObjectNotFoundException e) {
      return java.util.Optional.empty();
    }
  }

  @Override
  public boolean exists(String key) throws ObjectStoreException {
    try {
      client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        return false;
      }
      throw new ObjectStoreException("Failed to check existence: " + key, e);
    }
  }

  @Override
  public void delete(String key) throws ObjectStoreException {
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (S3Exception e) {
      throw new ObjectStoreException("Failed to delete object: " + key, e);
    }
  }

  @Override
  public ListResult list(String prefix, int limit, String continuationToken) throws ObjectStoreException {
    com.yourorg.objectstore.key.KeyValidator.validateLimit(limit);
    try {
      ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(prefix)
          .maxKeys(limit);
      if (continuationToken != null && !continuationToken.isBlank()) {
        request.continuationToken(continuationToken);
      }
      var response = client.listObjectsV2(request.build());
      List<ObjectInfo> items = response.contents().stream()
          .map(obj -> new ObjectInfo(
              obj.key(),
              obj.size(),
              obj.lastModified() == null ? Instant.EPOCH : obj.lastModified()))
          .toList();
      String nextToken = response.isTruncated() ? response.nextContinuationToken() : null;
      return new ListResult(items, nextToken);
    } catch (S3Exception e) {
      throw new ObjectStoreException("Failed to list objects", e);
    }
  }

  @Override
  public void close() {
  }
}
