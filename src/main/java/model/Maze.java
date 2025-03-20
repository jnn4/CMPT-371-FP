package main.java.model;

import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class Maze {
    private Vector<Vector<Square>> grid;
    private int rows, cols;
    private final ReentrantLock mazeLock = new ReentrantLock();

    public Maze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new Vector<>(rows);
        for(int i = 0; i < rows; i++) {
            Vector<Square> row = new Vector<>(cols);
            for(int j = 0; j < cols; j++) {
                row.add(new Square());
            }
            grid.add(row);
        }
    }

    public synchronized boolean isMovable(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < cols && !grid.get(x).get(y).isLocked();
    }

    public synchronized boolean tryLockSquare(int x, int y) {
        return grid.get(x).get(y).isLocked();
    }

    // do not use - do not need to unlock
    public void unlockSquare(int x, int y) {
        grid.get(x).get(y).unlock();
    }
}
