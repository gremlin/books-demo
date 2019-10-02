package com.mattrjacobs.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class CountingSemaphore {
    private final int limit;
    private final Semaphore underlying;

    private CountingSemaphore(final int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("CountingSemaphore limit must be at least 1");
        }
        this.limit = limit;
        this.underlying = new Semaphore(limit);
    }

    public static CountingSemaphore sized(final int limit) {
        return new CountingSemaphore(limit);
    }

    public <T> T wrap(Callable<T> f) {
        //System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " : about to acquire");
        final boolean acquired = underlying.tryAcquire(1);
        //System.out.println(System.currentTimeMillis() + " : " + Thread.currentThread().getName() + " : acquisition : " + acquired);
        if (!acquired) {
            throw new ConcurrencyLimitExceededException("Concurrency limit of " + limit + " exceeded");
        }
        try {
            return f.call();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            underlying.release(1);
        }
    }
}
