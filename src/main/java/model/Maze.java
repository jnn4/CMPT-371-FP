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
        // Maze of (rows x cols), with each cell being Square object
        for(int i = 0; i < rows; i++) {
            Vector<Square> row = new Vector<>(cols);
            for(int j = 0; j < cols; j++) {
                row.add(new Square());
            }
            grid.add(row);
        }
    }

    // within bounds and unlocked. Why not use tryLockSquare?
    public synchronized boolean isMovable(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < cols && !grid.get(x).get(y).isLocked();
    }

    // is this function trying to lock the square (in which case tryLock() be used instead)?
    // or is it just checking if the square is locked?
    public synchronized boolean tryLockSquare(int x, int y) {
        return grid.get(x).get(y).isLocked();
    }

    // do not use - do not need to unlock
    public void unlockSquare(int x, int y) {
        grid.get(x).get(y).unlock();
    }
}
