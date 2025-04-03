package main.java.model;

public class Grid {
    private final int size;
    private final Square[][] grid;

    public Grid(int size) {
        this.size = size;
        grid = new Square[size][size];
        for(int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = new Square();
            }
        }
    }

    public Square getSquare(int x, int y) {
        if (0 <= x && x < size && 0 <= y && y < size) {
            return grid[x][y];
        } else {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
    }

    public int getSize() {
        return size;
    }
}
