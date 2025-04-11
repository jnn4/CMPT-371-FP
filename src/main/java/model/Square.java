package main.java.model;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single square (or cell) on the game grid.
 * Each square can be owned (locked) by a player, act as a wall,
 * and has a corresponding JLabel for GUI display.
 */
public class Square {
    private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
    private volatile Player owner = null;
    private final JLabel label; // JLabel associated with this square in the UI
    private boolean isWall = false;
    private static final int LOCK_TIMEOUT_MS = 100; // Timeout for lock attempts

    /**
     * Constructs a Square with a given JLabel for display in the UI.
     *
     * @param label the JLabel representing this square visually
     */
    public Square(JLabel label) {
        this.label = label;
        this.label.setOpaque(true);
        clearSquare();
    }

    public boolean isWall() { return isWall; }
    // private int getX() { return label.getX(); }
    // private int getY() { return label.getY(); }
    public Player getOwner() { return owner; }

    /**
     * Clears the visual appearance of the square in the UI.
     */
    public synchronized void clearSquare() {
        SwingUtilities.invokeLater(() -> {
            label.setText("");
            label.setBackground(null);
        });
    }

    /**
     * Sets the wall status of this square.
     * Walls are permanently locked and cannot be entered.
     *
     * @param isWall true to set the square as a wall, false to make it a normal square
     */
    public synchronized void setWall(boolean isWall) {
        this.isWall = isWall;
        if (isWall) {
            try {
                if (lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    owner = null;
                    SwingUtilities.invokeLater(() -> {
                        label.setText("WALL");
                        label.setBackground(Color.DARK_GRAY);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            releaseLock(); // Unlock if it's no longer a wall
        }
    }


    public boolean isLocked() {
        return lock.isLocked() || isWall;
    }

    public boolean canEnter(Player player) {
        if (isWall) return false;
        return !isLocked() || (owner != null && owner.equals(player));
    }

    /**
     * Attempts to lock the square for the given player.
     *
     * @param player the player attempting to claim the square
     * @return true if the lock was acquired, false otherwise
     */
    public boolean tryLock(Player player) {
        if (player == null || isWall) return false;

        try {
            if (lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                owner = player;
                updateLabelForLock(player);
                // System.out.printf("Square at [%d,%d] locked by %s%n",
                        // X(), getY(), player.getId());
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // System.out.printf("Failed to lock square at [%d,%d] (locked by %s)%n",
                // getX(), getY(), (owner != null ? owner.getId() : "unknown"));
        return false;
    }

    /**
     * Releases the lock on this square if held by the current thread.
     * Resets ownership and updates UI.
     */
    public synchronized void releaseLock() {
        if (lock.isHeldByCurrentThread()) {
            // Player lastOwner = owner;
            owner = null;
            lock.unlock();
            // System.out.printf("Square at [%d,%d] unlocked (was held by %s)%n",
                    // getX(), getY(), (lastOwner != null ? lastOwner.getId() : "unknown"));
        } else if (lock.isLocked()) {
            System.err.printf("Thread %s attempted to release lock held by %s%n",
                    Thread.currentThread().getName(),
                    (owner != null ? owner.getId() : "unknown"));
        }
    }

    /**
     * Updates the UI to show the square is locked by a player.
     *
     * @param player the player who locked the square
     */
    private void updateLabelForLock(Player player) {
        SwingUtilities.invokeLater(() -> {
            // label.setText("L:" + player.getId());
            label.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        });
    }
}