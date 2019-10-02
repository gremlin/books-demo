package com.mattrjacobs.concurrency;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CountingSemaphoreTest {

    static int NUMBER_TRIALS = 1000;

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCountingSemaphore() {
        final CountingSemaphore semaphore = CountingSemaphore.sized(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroCountingSemaphore() {
        final CountingSemaphore semaphore = CountingSemaphore.sized(0);
    }

    @Test
    public void testSingleShotWithSemaphoreSizeOne() {
        final CountingSemaphore semaphore = CountingSemaphore.sized(1);

        final String result = semaphore.wrap(() -> "1");
        assertEquals("1", result);
    }

    @Test
    public void testSerialWithSemaphoreSizeOne() throws Throwable {
        final CountingSemaphore semaphore = CountingSemaphore.sized(1);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_TRIALS; i++) {
            final int stable = i;
            final Future<String> result = threadPool.submit(() ->
                    semaphore.wrap(() -> {
                        Thread.sleep(1);
                        return String.valueOf(stable);
                    }));
            futures.add(result);
        }
        try {
            for (final Future<String> future: futures) {
                assertNotNull(future.get());
            }
        } catch (ExecutionException ex) {
            System.out.println("ERROR : " + ex);
            throw ex.getCause();
        }
    }

    @Test(expected = ConcurrencyLimitExceededException.class)
    public void testParallelismOfFiveWithSemaphoreSizeOne() throws Throwable {
        final CountingSemaphore semaphore = CountingSemaphore.sized(1);
        final ExecutorService threadPool = Executors.newFixedThreadPool(5);
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_TRIALS; i++) {
            final int stable = i;
            final Future<String> result = threadPool.submit(() ->
                    semaphore.wrap(() -> {
                        Thread.sleep(1);
                        return String.valueOf(stable);
                    }));
            futures.add(result);
        }
        try {
            for (final Future<String> future: futures) {
                assertNotNull(future.get());
            }
        } catch (ExecutionException ex) {
            System.out.println("ERROR : " + ex);
            throw ex.getCause();
        }
    }

    @Test
    public void testSingleShotWithSemaphoreSizeFive() {
        final CountingSemaphore semaphore = CountingSemaphore.sized(5);

        final String result = semaphore.wrap(() -> "1");
        assertEquals("1", result);
    }

    @Test
    public void testSerialWithSemaphoreSizeFive() throws Throwable {
        final CountingSemaphore semaphore = CountingSemaphore.sized(5);
        final ExecutorService threadPool = Executors.newFixedThreadPool(1);
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_TRIALS; i++) {
            final int stable = i;
            final Future<String> result = threadPool.submit(() ->
                    semaphore.wrap(() -> {
                        Thread.sleep(1);
                        return String.valueOf(stable);
                    }));
            futures.add(result);
        }
        try {
            for (final Future<String> future: futures) {
                assertNotNull(future.get());
            }
        } catch (ExecutionException ex) {
            System.out.println("ERROR : " + ex);
            throw ex.getCause();
        }
    }

    @Test
    public void testParallelismOfFiveWithSemaphoreSizeFive() throws Throwable {
        final CountingSemaphore semaphore = CountingSemaphore.sized(5);
        final ExecutorService threadPool = Executors.newFixedThreadPool(5);
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_TRIALS; i++) {
            final int stable = i;
            final Future<String> result = threadPool.submit(() ->
                    semaphore.wrap(() -> {
                        Thread.sleep(1);
                        return String.valueOf(stable);
                    }));
            futures.add(result);
        }
        try {
            for (final Future<String> future: futures) {
                assertNotNull(future.get());
            }
        } catch (ExecutionException ex) {
            System.out.println("ERROR : " + ex);
            throw ex.getCause();
        }
    }

    @Test(expected = ConcurrencyLimitExceededException.class)
    public void testParallelismOfSixWithSemaphoreSizeFive() throws Throwable {
        final CountingSemaphore semaphore = CountingSemaphore.sized(5);
        final ExecutorService threadPool = Executors.newFixedThreadPool(6);
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_TRIALS; i++) {
            final int stable = i;
            final Future<String> result = threadPool.submit(() ->
                    semaphore.wrap(() -> {
                        Thread.sleep(1);
                        return String.valueOf(stable);
                    }));
            futures.add(result);
        }
        try {
            for (final Future<String> future: futures) {
                assertNotNull(future.get());
            }
        } catch (ExecutionException ex) {
            System.out.println("ERROR : " + ex);
            throw ex.getCause();
        }
    }
}
