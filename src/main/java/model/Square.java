package main.java.model;

import javax.swing.*;
import java.util.concurrent.locks.ReentrantLock;

public class Square {
    private final ReentrantLock lock = new ReentrantLock();
    private Player owner = null;
    private final JLabel label; // Assuming each square has a corresponding JLabel in the UI grid
    private boolean isWall = false;  // New flag to indicate if it's a wall


    public Square(JLabel label) {
        this.label = label;
    }

    // Clear the square (reset text and background)
    public void clearSquare() {
        label.setText(" ");
        label.setBackground(null);  // Reset to default background color
    }

    public boolean isWall() {
        return isWall;
    }

    public void setWall(boolean isWall) {
        this.isWall = isWall;
    }

    // Check if square is locked
    public boolean isLocked() {
        return lock.isLocked();
    }

    // Get player who owns the square (the one who locked it)
    public Player getOwner() {
        return owner;
    }

    // Check if square can be entered by the player
    public boolean canEnter(Player player) {
        // If the square is locked, only the owner can enter
        return !isLocked() || owner == player;
    }

    // Try to lock square for a player
    public boolean tryLock(Player player) {
        if (lock.tryLock()) {
            // If successful, the player owns the square
            owner = player;
            return true;
        }
        return false;
    }

    // Unlock the square after the move is complete
    public void releaseLock() {
        if (lock.isHeldByCurrentThread()) {
            owner = null;
            lock.unlock();
        } else {
            System.err.println("Attempt to release lock when the current thread does not hold it.");
        }
    }
}
