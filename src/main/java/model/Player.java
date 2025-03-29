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

        if(next.canEnter(this)) {
            next.tryLock(this);
            x = newX;
            y = newY;
            return true;
        }
        return false;
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
