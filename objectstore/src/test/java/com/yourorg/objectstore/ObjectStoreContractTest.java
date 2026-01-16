package com.yourorg.objectstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yourorg.objectstore.exceptions.ObjectAlreadyExistsException;
import com.yourorg.objectstore.exceptions.ObjectNotFoundException;
import com.yourorg.objectstore.exceptions.ObjectStoreException;
import com.yourorg.objectstore.factory.ObjectStoreFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ObjectStoreContractTest {
  private final ObjectStoreLimits smallLimits = new ObjectStoreLimits(8);
  private ObjectStore storeUnderTest;

  static Stream<Named<ObjectStore>> stores() {
    return Stream.of(
        Named.of("inMemory", ObjectStoreFactory.inMemory("env", ObjectStoreLimits.defaults())),
        Named.of("localDisk", ObjectStoreFactory.localDisk("test-" + UUID.randomUUID(), "env", ObjectStoreLimits.defaults()))
    );
  }

  @AfterEach
  void tearDown() throws Exception {
    if (storeUnderTest != null) {
      storeUnderTest.close();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void createOnlyPut(ObjectStore store) throws Exception {
    storeUnderTest = store;
    store.put("alpha/key.txt", "data".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    assertThrows(ObjectAlreadyExistsException.class, () ->
        store.put("alpha/key.txt", "data".getBytes(StandardCharsets.UTF_8), PutOptions.defaults()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void streamingPutGetRoundtrip(ObjectStore store) throws Exception {
    storeUnderTest = store;
    byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
    store.put("alpha/stream", payload, PutOptions.defaults());
    try (InputStream input = store.get("alpha/stream")) {
      byte[] read = input.readAllBytes();
      assertArrayEquals(payload, read);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void getNotFound(ObjectStore store) throws Exception {
    storeUnderTest = store;
    assertThrows(ObjectNotFoundException.class, () -> store.get("missing/file"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void existsLifecycle(ObjectStore store) throws Exception {
    storeUnderTest = store;
    assertFalse(store.exists("alpha/exists"));
    store.put("alpha/exists", "ok".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    assertTrue(store.exists("alpha/exists"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void deleteIsIdempotent(ObjectStore store) throws Exception {
    storeUnderTest = store;
    store.delete("alpha/delete");
    store.put("alpha/delete", "ok".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    store.delete("alpha/delete");
    assertFalse(store.exists("alpha/delete"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void listPrefixFiltering(ObjectStore store) throws Exception {
    storeUnderTest = store;
    store.put("alpha/one", "1".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    store.put("alpha/two", "2".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    store.put("beta/three", "3".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());

    ListResult result = store.list("alpha/", 10, null);
    assertEquals(2, result.items().size());
    assertTrue(result.items().stream().allMatch(item -> item.key().startsWith("alpha/")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void listPagination(ObjectStore store) throws Exception {
    storeUnderTest = store;
    store.put("alpha/a", "1".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    store.put("alpha/b", "2".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    store.put("alpha/c", "3".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());

    ListResult page1 = store.list("alpha/", 2, null);
    assertEquals(2, page1.items().size());
    assertNotNull(page1.nextContinuationToken());

    ListResult page2 = store.list("alpha/", 2, page1.nextContinuationToken());
    assertEquals(1, page2.items().size());
    assertTrue(page2.nextContinuationToken() == null || page2.nextContinuationToken().isBlank());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("stores")
  void prefixDecoratorRejectsPrefixed(ObjectStore store) throws Exception {
    storeUnderTest = store;
    assertThrows(IllegalArgumentException.class, () -> store.put("env/already", "x".getBytes(StandardCharsets.UTF_8), PutOptions.defaults()));
    assertThrows(IllegalArgumentException.class, () -> store.list("env/", 10, null));
  }

  @Test
  void getBytesBounded() throws Exception {
    storeUnderTest = ObjectStoreFactory.inMemory("env", smallLimits);
    storeUnderTest.put("alpha/small", "ok".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), storeUnderTest.getBytes("alpha/small"));

    storeUnderTest.put("alpha/large", "toolarge".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    assertThrows(ObjectStoreException.class, () -> storeUnderTest.getBytes("alpha/large"));
  }

  @Test
  void getIfExistsReturnsOptional() throws Exception {
    storeUnderTest = ObjectStoreFactory.inMemory("env", ObjectStoreLimits.defaults());
    Optional<InputStream> missing = storeUnderTest.getIfExists("alpha/missing");
    assertTrue(missing.isEmpty());

    storeUnderTest.put("alpha/present", "data".getBytes(StandardCharsets.UTF_8), PutOptions.defaults());
    Optional<InputStream> present = storeUnderTest.getIfExists("alpha/present");
    assertTrue(present.isPresent());
    try (InputStream input = present.get()) {
      assertArrayEquals("data".getBytes(StandardCharsets.UTF_8), input.readAllBytes());
    }
  }
}
