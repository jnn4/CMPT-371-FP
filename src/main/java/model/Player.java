package main.java.model;

public class Player {
    private String id; // unique identifier
    private int x, y;
    private String color;

    public Player(String id, int startX, int startY, String color) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.color = color;
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

    public boolean move(int newX, int newY, Maze maze) {
        Square current = maze.getSquare(x, y);
        Square next = maze.getSquare(newX, newY);

        if(next.canEnter(this)) {
            current.unlock();
            next.lock(this);
            x = newX;
            y = newY;
            return true;
        }
        return false;
    }
}
