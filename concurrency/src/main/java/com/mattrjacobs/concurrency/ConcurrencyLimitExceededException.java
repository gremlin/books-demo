package com.mattrjacobs.concurrency;

public class ConcurrencyLimitExceededException extends RuntimeException {
    ConcurrencyLimitExceededException(String msg) {
        super(msg);
    }
}
