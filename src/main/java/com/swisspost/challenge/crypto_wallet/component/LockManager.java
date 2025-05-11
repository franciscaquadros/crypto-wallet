package com.swisspost.challenge.crypto_wallet.component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Component;

@Component
public class LockManager {

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  public Lock getReadLock() {
    return readLock;
  }

  public Lock getWriteLock() {
    return writeLock;
  }
}
