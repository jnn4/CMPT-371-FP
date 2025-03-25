package main.java.model;

import java.util.Vector;

public class Grid {
    private final int size;
    private Vector<Vector<Square>> grid;

    public Grid(int size) {
        this.size = size;
        grid = new Vector<>(size);
        for(int i = 0; i < size; i++) {
            Vector<Square> row = new Vector<>(size);
            for(int j = 0; j < size; j++) {
                row.add(new Square());
            }
            grid.add(row);
        }

    }

    public Square getSquare(int x, int y) {
        return grid.get(x).get(y);
    }

    public int getSize() {
        return size;
    }
}
