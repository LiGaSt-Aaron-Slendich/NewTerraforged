package com.terraforged.mod.util.storage;

import java.util.concurrent.locks.StampedLock;

public class LockUtil {
    public static void unlockIfRead(StampedLock lock, long readStamp) {
        if (StampedLock.isReadLockStamp(readStamp)) {
            lock.unlockRead(readStamp);
        }
    }

    public static long convertToWrite(StampedLock lock, long readStamp) {
        if (StampedLock.isReadLockStamp(readStamp)) {
            lock.unlockRead(readStamp);
        }
        return lock.writeLock();
    }
}
