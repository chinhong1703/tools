package com.example.csvgenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CsvGeneratorClient {
    private final CsvGenerator generator;

    public CsvGeneratorClient(CsvGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    public List<CsvGenerationResult> generateAll(List<CsvGenerationRequest<?>> requests,
                                                 CsvGeneratorOptions generatorOptions,
                                                 CsvGeneratorClientOptions clientOptions) throws IOException {
        Objects.requireNonNull(requests, "requests");
        Objects.requireNonNull(generatorOptions, "generatorOptions");
        Objects.requireNonNull(clientOptions, "clientOptions");

        if (clientOptions.maxParallelism() == 1 || requests.size() <= 1) {
            List<CsvGenerationResult> results = new ArrayList<>();
            for (CsvGenerationRequest<?> request : requests) {
                results.add(generator.generate(castRequest(request), generatorOptions));
            }
            return results;
        }

        ExecutorService executor = new ThreadPoolExecutor(
                clientOptions.maxParallelism(),
                clientOptions.maxParallelism(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(clientOptions.queueSize()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            List<Future<CsvGenerationResult>> futures = new ArrayList<>();
            for (CsvGenerationRequest<?> request : requests) {
                futures.add(executor.submit(() -> generator.generate(castRequest(request), generatorOptions)));
            }

            List<CsvGenerationResult> results = new ArrayList<>();
            for (Future<CsvGenerationResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Generation interrupted", ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof IOException ioException) {
                        throw ioException;
                    }
                    throw new IOException("Generation failed", cause);
                }
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CsvGenerationRequest<T> castRequest(CsvGenerationRequest<?> request) {
        return (CsvGenerationRequest<T>) request;
    }
}
