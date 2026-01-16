package com.yourorg.objectstore.impl.local;

import com.yourorg.objectstore.ListResult;
import com.yourorg.objectstore.ObjectInfo;
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class LocalDiskObjectStore implements ObjectStore {
  private final Path baseDir;

  public LocalDiskObjectStore(String storeName) {
    if (storeName == null || storeName.isBlank()) {
      throw new IllegalArgumentException("storeName must not be blank");
    }
    String tmpDir = System.getProperty("java.io.tmpdir");
    this.baseDir = Path.of(tmpDir, "objectstore", storeName);
  }

  @Override
  public void put(String key, InputStream data, long contentLength, PutOptions options)
      throws ObjectAlreadyExistsException, ObjectStoreException {
    ensureBaseDir();
    Path path = resolvePath(key);
    try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      data.transferTo(output);
    } catch (java.nio.file.FileAlreadyExistsException e) {
      throw new ObjectAlreadyExistsException("Object already exists: " + key, e);
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to write object: " + key, e);
    }
  }

  @Override
  public InputStream get(String key) throws ObjectNotFoundException, ObjectStoreException {
    Path path = resolvePath(key);
    try {
      return Files.newInputStream(path, StandardOpenOption.READ);
    } catch (java.nio.file.NoSuchFileException e) {
      throw new ObjectNotFoundException("Object not found: " + key, e);
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to read object: " + key, e);
    }
  }

  @Override
  public byte[] getBytes(String key) throws ObjectNotFoundException, ObjectStoreException {
    Path path = resolvePath(key);
    try {
      return Files.readAllBytes(path);
    } catch (java.nio.file.NoSuchFileException e) {
      throw new ObjectNotFoundException("Object not found: " + key, e);
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to read object: " + key, e);
    }
  }

  @Override
  public Optional<InputStream> getIfExists(String key) throws ObjectStoreException {
    if (!Files.exists(resolvePath(key))) {
      return Optional.empty();
    }
    return Optional.of(get(key));
  }

  @Override
  public boolean exists(String key) {
    return Files.exists(resolvePath(key));
  }

  @Override
  public void delete(String key) throws ObjectStoreException {
    Path path = resolvePath(key);
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to delete object: " + key, e);
    }
  }

  @Override
  public ListResult list(String prefix, int limit, String continuationToken) throws ObjectStoreException {
    com.yourorg.objectstore.key.KeyValidator.validateLimit(limit);
    ensureBaseDir();
    List<PathEntry> entries = new ArrayList<>();
    try (Stream<Path> stream = Files.list(baseDir)) {
      stream.forEach(path -> {
        String key = decodeKey(path.getFileName().toString());
        if (key.startsWith(prefix)) {
          entries.add(new PathEntry(key, path));
        }
      });
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to list objects", e);
    }

    entries.sort(Comparator.comparing(PathEntry::key));

    int startIndex = 0;
    if (continuationToken != null && !continuationToken.isBlank()) {
      for (int i = 0; i < entries.size(); i++) {
        if (entries.get(i).key().compareTo(continuationToken) > 0) {
          startIndex = i;
          break;
        }
        startIndex = entries.size();
      }
    }

    int endIndex = Math.min(startIndex + limit, entries.size());
    List<ObjectInfo> items = new ArrayList<>();
    for (int i = startIndex; i < endIndex; i++) {
      PathEntry entry = entries.get(i);
      try {
        long size = Files.size(entry.path());
        Instant lastModified = Files.getLastModifiedTime(entry.path()).toInstant();
        items.add(new ObjectInfo(entry.key(), size, lastModified));
      } catch (IOException e) {
        throw new ObjectStoreException("Failed to read metadata for " + entry.key(), e);
      }
    }

    String nextToken = endIndex < entries.size() ? entries.get(endIndex - 1).key() : null;
    return new ListResult(items, nextToken);
  }

  @Override
  public void close() {
  }

  private void ensureBaseDir() throws ObjectStoreException {
    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new ObjectStoreException("Failed to create base directory", e);
    }
  }

  private Path resolvePath(String key) {
    return baseDir.resolve(encodeKey(key));
  }

  private String encodeKey(String key) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
  }

  private String decodeKey(String fileName) {
    return new String(Base64.getUrlDecoder().decode(fileName), StandardCharsets.UTF_8);
  }

  private record PathEntry(String key, Path path) {
  }
}
