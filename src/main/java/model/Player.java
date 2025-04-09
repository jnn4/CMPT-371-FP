package main.java.model;

public class Player {
    private String id; // unique identifier
    private int x, y;
    private String color;
    private boolean ready = false;

    public Player(String id, int startX, int startY, String color) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.color = color;
        this.ready = false;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getColor() {
        return color;
    }

    public boolean getReady() {
        return ready;
    }

    public boolean move(int newX, int newY, Grid grid) {
        Square next = grid.getSquare(newX, newY);

        // If the square is a wall, the player cannot move there
        if (next.isWall()) {
            return false; // Move blocked if the square is a wall
        }

        // Before the move, mark the current square as a wall (trail left behind)
        Square current = grid.getSquare(x, y);
        current.setWall(true);

        // If the player can enter the new square, move there
        if (next.canEnter(this)) {
            next.tryLock(this);  // Lock the new square for the player
            x = newX;  // Update the player's x-coordinate
            y = newY;  // Update the player's y-coordinate
            return true;  // Successful move
        }

        return false;  // Move failed
    }

    public void setX(int newX) {
        this.x = newX;
    }

    public void setY(int newY) {
        this.y = newY;
    }

    public void toggleReady() {
        this.ready = !this.ready;
    }
}
