package model;

public class Player {
    private int x, y;
    private Maze maze;
    private String name;

    public Player(String name, int startX, int startY, Maze maze) {
        this.name = name;
        this.x = startX;
        this.y = startY;
        this.maze = maze;
        maze.tryLockSquare(x, y);
    }

    public boolean move(int dx, int dy) {
        synchronized (this) {
            if (maze.isMovable(dx, dy) && maze.tryLockSquare(dx, dy)) {
               x = dx;
               y = dy;
               return true;
            } else {
                System.out.println(name + " is not movable");
            }
        }
        return false;
    }

    public void run() {
        // Example random movement loop
        for (int i = 0; i < 5; i++) {
            int newX = x + (Math.random() > 0.5 ? 1 : -1);
            int newY = y + (Math.random() > 0.5 ? 1 : -1);
            move(newX, newY);

            try {
                Thread.sleep(500); // Simulate movement delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
