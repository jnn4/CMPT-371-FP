package main.java.model;

import java.util.Objects;

/**
 * Represents a player in the game with a unique ID, position, and color.
 * Tracks the player's readiness state and allows movement on a grid.
 */
public class Player {
    private String id; // Unique identifier
    private int x, y;
    private String color;
    private boolean ready = false;

    /**
     * Constructs a new Player.
     *
     * @param id    the unique identifier for the player (must not be null or empty)
     * @param x     the initial x-coordinate (must be non-negative)
     * @param y     the initial y-coordinate (must be non-negative)
     * @param color the player's color (must not be null)
     * @throws IllegalArgumentException if id is null/empty, or coordinates are invalid
     * @throws NullPointerException     if color is null
     */
    public Player(String id, int x, int y, String color) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Player ID cannot be null/empty");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Invalid position coordinates");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = Objects.requireNonNull(color, "Color cannot be null");
        System.out.println("Created Player: " + id + " at (" + x + "," + y + ")");
    }

    // Getters & Setters
    public String getId() { return id;}
    public int getX() { return x; }
    public int getY() { return y; }
    public String getColor() { return color;}
    public boolean getReady() { return ready;}
    public void setX(int newX) { this.x = newX;}
    public void setY(int newY) { this.y = newY;}
    public void toggleReady() { this.ready = !this.ready;}

    /**
     * Attempts to move the player to the specified position on the grid.
     * The player cannot move into a wall or a locked square.
     *
     * @param newX the target x-coordinate
     * @param newY the target y-coordinate
     * @param grid the grid on which the player moves
     * @return true if the move was successful, false otherwise
     */
    public boolean move(int newX, int newY, Grid grid) {
        Square next = grid.getSquare(newX, newY);

        if (next.isWall()) {
            return false;
        }

        Square current = grid.getSquare(x, y);
        current.releaseLock();
        current.setWall(false);

        if (next.canEnter(this)) {
            next.tryLock(this);
            x = newX;
            y = newY;
            return true;
        }

        return false;
    }
}
