package main.java.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Square {
    private boolean isLocked;
    private final Lock lock;

    public Square() {
        this.isLocked = false;
        this.lock = new ReentrantLock();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public synchronized boolean tryLock() {
        if(lock.tryLock()) {
            isLocked = true;
            return true;
        }
        return false;
    }

    // should not use for shared squares??
    public void unlock() {
        if(isLocked) {
            isLocked = false;
            lock.unlock();
        }
    }
}
