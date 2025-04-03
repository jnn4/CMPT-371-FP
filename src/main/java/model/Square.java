package main.java.model;

import java.util.concurrent.locks.ReentrantLock;

public class Square {
    private final ReentrantLock lock = new ReentrantLock();
    private Player owner = null;

    // check if square is locked
    public boolean isLocked() {
        return lock.isLocked();
    }

    // get player who owns the square (the one who locked it)
    public Player getOwner() {
        return owner;
    }

    // check if square can be entered by the player
    public boolean canEnter(Player player) {
        // if locked, only owner can enter
        return !isLocked() || owner == player;
    }

    // try to lock square for a player
    public boolean tryLock(Player player) {
        if(lock.tryLock()) {
            owner = player;
            return true;
        }
        return false;
    }

    // do not use
    public void unlock() {
        if(lock.isHeldByCurrentThread()) {
            owner = null;
            lock.unlock();
        }
    }

}
