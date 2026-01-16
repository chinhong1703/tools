# ObjectStore

A small Java 21 library that provides a consistent `ObjectStore` API backed by S3, local disk (temp directory), or in-memory storage. It supports streaming reads/writes, create-only puts, idempotent deletes, prefix-based listing with pagination, and a bounded `getBytes` convenience method.

## Features

- **Single interface** for S3, local disk, and in-memory stores
- **Streaming I/O** for `put` and `get`
- **Create-only put**: fails if the key already exists
- **Idempotent delete**: deleting missing keys does not error
- **Prefix-based list** with S3-style pagination
- **Key prefixing** to segregate environments (e.g., `dev/`, `staging/`)
- **Bounded getBytes** to avoid accidental OOMs

## Requirements

- Java 21
- Maven

## Usage

### Create a store via the factory

```java
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.ObjectStoreLimits;
import com.yourorg.objectstore.PutOptions;
import com.yourorg.objectstore.factory.ObjectStoreFactory;
import java.io.InputStream;

ObjectStore store = ObjectStoreFactory.localDisk(
    "my-app",
    "dev",
    ObjectStoreLimits.defaults()
);

store.put("images/logo.png", "data".getBytes(), PutOptions.defaults());

try (InputStream input = store.get("images/logo.png")) {
  byte[] bytes = input.readAllBytes();
}

store.delete("images/logo.png");
store.close();
```

### Create an S3-backed store

```java
import com.yourorg.objectstore.ObjectStore;
import com.yourorg.objectstore.ObjectStoreLimits;
import com.yourorg.objectstore.factory.ObjectStoreFactory;
import software.amazon.awssdk.services.s3.S3Client;

S3Client client = S3Client.builder().build();
ObjectStore store = ObjectStoreFactory.s3(client, "my-bucket", "prod", ObjectStoreLimits.defaults());

store.put("reports/2024-01.csv", "data".getBytes(), com.yourorg.objectstore.PutOptions.defaults());
```

### List objects with pagination

```java
import com.yourorg.objectstore.ListResult;

String token = null;
int limit = 2;

while (true) {
  ListResult result = store.list("reports/", limit, token);
  result.items().forEach(item -> System.out.println(item.key()));
  if (result.nextContinuationToken() == null || result.nextContinuationToken().isBlank()) {
    break;
  }
  token = result.nextContinuationToken();
}
```

### Bounded getBytes

```java
import com.yourorg.objectstore.ObjectStoreLimits;

ObjectStore store = ObjectStoreFactory.inMemory("test", new ObjectStoreLimits(1024));
byte[] data = store.getBytes("small/object");
```

## Key prefixing

When a prefix is provided, it is normalized to end in `/`. Callers always use logical keys (without the prefix). Passing already-prefixed keys is rejected.

Examples:

- Prefix `"dev"` normalizes to `"dev/"`.
- `put("logs/app.log", ...)` writes to the physical key `dev/logs/app.log`.

## Build and test

```bash
mvn -f objectstore/pom.xml test
```
